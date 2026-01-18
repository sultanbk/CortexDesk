import os
import requests
import openai
from openai import AzureOpenAI
from dotenv import load_dotenv

load_dotenv()

# Config
AZURE_SEARCH_ENDPOINT = os.getenv("AZURE_SEARCH_ENDPOINT")
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
# prefer specific embedding deployment env var, fall back to generic deployment name
AZURE_OPENAI_DEPLOYMENT = _getenv_clean("AZURE_OPENAI_DEPLOYMENT") or _getenv_clean("AZURE_DEPLOYMENT_NAME")
AZURE_OPENAI_API_VERSION = "2024-02-15-preview"

def _deployment_candidates(deployment: str | None):
    candidates = []
    if deployment:
        candidates.append(deployment)
        if "_" in deployment:
            candidates.append(deployment.replace("_", "-"))
        if "-" in deployment:
            candidates.append(deployment.replace("-", "_"))
    return [c for i, c in enumerate(candidates) if c and c not in candidates[:i]]

# Clients
openai_client = AzureOpenAI(
    api_key=AZURE_OPENAI_API_KEY,
    api_version=AZURE_OPENAI_API_VERSION,
    azure_endpoint=AZURE_OPENAI_ENDPOINT
)

# Debug: show sanitized values
# print("[knoweledge_base_tool] AZURE_OPENAI_ENDPOINT:", AZURE_OPENAI_ENDPOINT)
# print("[knoweledge_base_tool] AZURE_OPENAI_DEPLOYMENT (sanitized):", AZURE_OPENAI_DEPLOYMENT)
# print("[knoweledge_base_tool] Deployment candidates:", _deployment_candidates(AZURE_OPENAI_DEPLOYMENT))

def embed_text(text: str):
    candidates = _deployment_candidates(AZURE_OPENAI_DEPLOYMENT)
    last_exc = None
    tried = []
    for model in candidates:
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

    print(f"❌ Deployment not found. Tried candidates: {tried}")
    print("Verify AZURE_OPENAI_DEPLOYMENT/AZURE_DEPLOYMENT_NAME in your .env and that the deployment exists in Azure OpenAI (no quotes).")
    if last_exc:
        raise last_exc
    raise RuntimeError("Embedding deployment not found and no further details available.")

def search_similar_solution(query: str, category: str) -> str:
    embedding = embed_text(query)

    url = f"{AZURE_SEARCH_ENDPOINT}/indexes/{AZURE_SEARCH_INDEX_NAME}/docs/search?api-version=2023-07-01-Preview"

    headers = {
        "Content-Type": "application/json",
        "api-key": AZURE_SEARCH_KEY
    }

    payload = {
        "search": "",
        "vectors": [
            {
                "value": embedding,
                "fields": "embedding",
                "k": 3
            }
        ],
        "select": "category,problem,solution",
        "filter": f"category eq '{category}'"
    }

    response = requests.post(url, headers=headers, json=payload)
    if response.status_code != 200:
        return f"Error while searching: {response.text}"

    results = response.json().get("value", [])

    if not results:
        return "No matching solutions found."

    response_text = ""
    for idx, doc in enumerate(results, 1):
        response_text += (
            f"\nResult {idx}:\n"
            f"Category: {doc.get('category')}\n"
            f"Problem: {doc.get('problem')}\n"
            f"Solution: {doc.get('solution')}\n"
        )

    return response_text

if __name__ == "__main__":
    query = "The LAN cable is plugged in but the computer says 'Cable Disconnected'."
    rag_output = search_similar_solution(query, category="No Internet")
    print(rag_output)