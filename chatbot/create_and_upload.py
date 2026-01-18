
import os
import json
from tqdm import tqdm
from azure.core.credentials import AzureKeyCredential
from azure.search.documents.indexes import SearchIndexClient
from azure.search.documents.indexes.models import (
    SearchIndex,
    SearchField,
    SearchFieldDataType,
    SimpleField,
    SearchableField,
    VectorSearch,
    HnswAlgorithmConfiguration,
    VectorSearchProfile
)
from azure.search.documents import SearchClient
import openai
from openai import AzureOpenAI
from dotenv import load_dotenv

load_dotenv()

# ---------------- CONFIG ----------------
AZURE_SEARCH_ENDPOINT = os.getenv("AZURE_SEARCH_ENDPOINT")  # e.g., https://your-search.search.windows.net
AZURE_SEARCH_KEY = os.getenv("AZURE_SEARCH_KEY")
AZURE_SEARCH_INDEX_NAME = "it-ticket-solutions-index"

def _getenv_clean(key: str, default: str | None = None) -> str | None:
    val = os.getenv(key, default)
    if val is None:
        return None
    val = val.strip()
    if (val.startswith('"') and val.endswith('"')) or (val.startswith("'") and val.endswith("'")):
        val = val[1:-1].strip()
    return val

AZURE_OPENAI_API_KEY = _getenv_clean("AZURE_OPENAI_API_KEY")
AZURE_OPENAI_ENDPOINT = _getenv_clean("AZURE_OPENAI_ENDPOINT")
# Prefer specific embedding deployment env var, fall back to generic deployment name
AZURE_OPENAI_DEPLOYMENT = _getenv_clean("AZURE_OPENAI_DEPLOYMENT") or _getenv_clean("AZURE_DEPLOYMENT_NAME")
AZURE_OPENAI_API_VERSION = "2024-02-15-preview"

if not AZURE_OPENAI_DEPLOYMENT:
    raise ValueError(
        "AZURE_OPENAI_DEPLOYMENT (or AZURE_DEPLOYMENT_NAME) is not set in the environment."
        " Set it to the exact deployment name (no quotes) used in your Azure OpenAI resource."
    )


def _deployment_candidates():
    candidates = []
    if AZURE_OPENAI_DEPLOYMENT:
        candidates.append(AZURE_OPENAI_DEPLOYMENT)
        if "_" in AZURE_OPENAI_DEPLOYMENT:
            candidates.append(AZURE_OPENAI_DEPLOYMENT.replace("_", "-"))
        if "-" in AZURE_OPENAI_DEPLOYMENT:
            candidates.append(AZURE_OPENAI_DEPLOYMENT.replace("-", "_"))
    fallback = _getenv_clean("AZURE_DEPLOYMENT_NAME")
    if fallback and fallback not in candidates:
        candidates.append(fallback)
    # dedupe while preserving order
    seen = set()
    out = []
    for c in candidates:
        if c and c not in seen:
            seen.add(c)
            out.append(c)
    return out

DATA_FILE = "data/knoweledge_base.json"
VECTOR_DIMENSIONS = 1536  # Based on OpenAI embeddings
# ----------------------------------------

# ---------- Clients ----------
credential = AzureKeyCredential(AZURE_SEARCH_KEY)
index_client = SearchIndexClient(endpoint=AZURE_SEARCH_ENDPOINT, credential=credential)
search_client = SearchClient(endpoint=AZURE_SEARCH_ENDPOINT, index_name=AZURE_SEARCH_INDEX_NAME, credential=credential)
openai_client = AzureOpenAI(api_key=AZURE_OPENAI_API_KEY, api_version=AZURE_OPENAI_API_VERSION, azure_endpoint=AZURE_OPENAI_ENDPOINT)


# ---------- Index Creation ----------
def create_index():
    print("📦 Creating index...")

    fields = [
        SimpleField(name="id", type=SearchFieldDataType.String, key=True, filterable=True, sortable=True, facetable=True),
        SearchableField(name="category", type=SearchFieldDataType.String, filterable=True, sortable=True, facetable=True),
        SearchableField(name="problem", type=SearchFieldDataType.String, filterable=True, sortable=True, facetable=True),
        SearchableField(name="solution", type=SearchFieldDataType.String, filterable=True, sortable=True, facetable=True),
        SearchField(
            name="embedding",
            type=SearchFieldDataType.Collection(SearchFieldDataType.Single),
            searchable=True,
            vector_search_dimensions=VECTOR_DIMENSIONS,
            vector_search_profile_name="default"
        )
    ]

    vector_search = VectorSearch(
        algorithms=[HnswAlgorithmConfiguration(name="default")],
        profiles=[VectorSearchProfile(name="default", algorithm_configuration_name="default")]
    )

    index = SearchIndex(name=AZURE_SEARCH_INDEX_NAME, fields=fields, vector_search=vector_search)

    try:
        # Try to get the index; if it exists, we skip creation
        index_client.get_index(name=AZURE_SEARCH_INDEX_NAME)
        print(f"ℹ️ Index '{AZURE_SEARCH_INDEX_NAME}' already exists. Skipping creation.")
    except Exception:
        # If not found, create the index
        index_client.create_index(index)
        print(f"✅ Index '{AZURE_SEARCH_INDEX_NAME}' created successfully.")


# ---------- Embedding ----------
def embed_text(text: str):
    # Try a list of candidate deployment/model names to handle common naming differences
    candidates = []
    if AZURE_OPENAI_DEPLOYMENT:
        candidates.append(AZURE_OPENAI_DEPLOYMENT)
        # common variants
        if "_" in AZURE_OPENAI_DEPLOYMENT:
            candidates.append(AZURE_OPENAI_DEPLOYMENT.replace("_", "-"))
        if "-" in AZURE_OPENAI_DEPLOYMENT:
            candidates.append(AZURE_OPENAI_DEPLOYMENT.replace("-", "_"))

    fallback = _getenv_clean("AZURE_DEPLOYMENT_NAME")
    if fallback and fallback not in candidates:
        candidates.append(fallback)

    last_exc = None
    tried = []
    for model in candidates:
        if not model:
            continue
        tried.append(model)
        try:
            response = openai_client.embeddings.create(
                input=[text],
                model=model
            )
            return response.data[0].embedding
        except openai.NotFoundError as e:
            last_exc = e
            continue

    # Nothing worked — give a helpful error listing what we tried
    print(f"❌ Deployment not found. Tried candidates: {tried}")
    print("Verify the deployment names in your Azure OpenAI resource and update AZURE_OPENAI_DEPLOYMENT in your .env (no quotes).")
    if last_exc:
        raise last_exc
    raise RuntimeError("Embedding deployment not found and no further details available.")


# ---------- Upload ----------
def load_data():
    with open(DATA_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def upload_documents(docs):
    print(f"\n📤 Uploading {len(docs)} documents...")
    batch_size = 10
    for i in tqdm(range(0, len(docs), batch_size)):
        chunk = docs[i:i+batch_size]
        search_client.upload_documents(documents=chunk)
    print("✅ Upload completed.")

# ---------- Main ----------
def main():
    # Debug: print sanitized env values so user can verify exact deployment names
    masked_key = AZURE_OPENAI_API_KEY[:4] + "..." if AZURE_OPENAI_API_KEY else None
    print("--- Debug env values ---")
    print(f"AZURE_OPENAI_ENDPOINT: {AZURE_OPENAI_ENDPOINT}")
    print(f"AZURE_OPENAI_API_VERSION: {AZURE_OPENAI_API_VERSION}")
    print(f"AZURE_OPENAI_DEPLOYMENT (sanitized): {AZURE_OPENAI_DEPLOYMENT}")
    print(f"AZURE_OPENAI_API_KEY: {masked_key}")
    print(f"Deployment candidates: {_deployment_candidates()}")
    print("--- End debug ---")

    create_index()

    raw_docs = load_data()
    enriched_docs = []

    print("🔍 Generating embeddings...")
    for doc in tqdm(raw_docs):
        embedding = embed_text(doc["problem"])
        doc["embedding"] = embedding
        enriched_docs.append(doc)

    upload_documents(enriched_docs)


if __name__ == "__main__":
    main()