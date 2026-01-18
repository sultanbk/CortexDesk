import csv
import json

csv_file_path = 'data/dummy_tickets.csv'
json_file_path = 'tickets_for_embedding.json'

data = []

with open(csv_file_path, encoding='utf-8') as csvf:
    csv_reader = csv.DictReader(csvf)
    for row in csv_reader:
        # Mapping the specific fields you requested
        ticket = {
            "id": row["ticket_id"],
            "category": row["issue_category_id"],
            "problem": row["description"],
            "solution": row["resolution_summary"]
        }
        data.append(ticket)

with open(json_file_path, 'w', encoding='utf-8') as jsonf:
    jsonf.write(json.dumps(data, indent=4))

print(f"Successfully converted to {json_file_path}")