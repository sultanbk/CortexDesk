import json
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer

DATA_PATH = "../data/tickets.json"
INDEX_PATH = "index.faiss"
MODEL_NAME = "all-MiniLM-L6-v2"

def ticket_to_text(ticket):
    return f"""
    Problem: {ticket.get('problem', '')}
    Category: {ticket.get('category', '')}
    Solution: {ticket.get('solution', '')}
    """

def main():
    print("Loading tickets...")
    with open(DATA_PATH, "r", encoding="utf-8") as f:
        tickets = json.load(f)

    texts = [ticket_to_text(t) for t in tickets]

    print("Loading embedding model...")
    model = SentenceTransformer(MODEL_NAME)

    print("Generating embeddings...")
    embeddings = model.encode(texts, show_progress_bar=True)

    dimension = embeddings.shape[1]
    index = faiss.IndexFlatL2(dimension)
    index.add(np.array(embeddings))

    faiss.write_index(index, INDEX_PATH)
    print(f"FAISS index created for {len(tickets)} tickets")

if __name__ == "__main__":
    main()
