# Chatbot - AI-Powered Support Agent (Python + AutoGen)

Intelligent multi-agent system using Microsoft AutoGen framework to provide AI-powered technical support with knowledge base retrieval and intent classification.

## 🎯 Overview

This Python chatbot provides:
- Automatic intent classification (13+ ticket categories)
- Semantic search through historical solutions
- Multi-agent orchestration with AutoGen
- Azure OpenAI integration
- FAISS vector database for fast similarity search
- REST API endpoint for web integration
- Optional Streamlit web UI

## 📋 Prerequisites

### System Requirements
- **Python 3.8 or higher**
- **pip 20.0 or higher**
- **Git** (for cloning)
- **Azure OpenAI API key** (for AI capabilities)

### Verify Installation
```bash
python --version        # Should show Python 3.8+
pip --version          # Should show pip 20.0+
```

## 🚀 Quick Start

### 1. Create Python Virtual Environment (Recommended)

```bash
cd chatbot

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate

# On macOS/Linux:
source venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

> This may take a minute to download packages (~300MB)

### 3. Configure Azure OpenAI

Create `.env` file in chatbot folder:

```bash
# Windows (PowerShell)
echo @'
AZURE_OPENAI_API_KEY=your-api-key-here
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_DEPLOYMENT_NAME=gpt-4-deployment-name
AZURE_API_VERSION=2024-02-15-preview
AZURE_SEARCH_ENDPOINT=https://your-search.search.windows.net/
AZURE_SEARCH_KEY=your-search-key-here
AZURE_SEARCH_INDEX_NAME=it-ticket-solutions-index
'@ | Out-File .env -Encoding utf8

# macOS/Linux
cat > .env << 'EOF'
AZURE_OPENAI_API_KEY=your-api-key-here
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_DEPLOYMENT_NAME=gpt-4-deployment-name
AZURE_API_VERSION=2024-02-15-preview
AZURE_SEARCH_ENDPOINT=https://your-search.search.windows.net/
AZURE_SEARCH_KEY=your-search-key-here
AZURE_SEARCH_INDEX_NAME=it-ticket-solutions-index
EOF
```

> **Getting Azure OpenAI Credentials**:
> 1. Go to [Azure Portal](https://portal.azure.com/)
> 2. Create or find your OpenAI resource
> 3. Navigate to Keys & Endpoints
> 4. Copy API Key and Endpoint URL
> 5. Deployment name is the name of your deployed model (e.g., "gpt-4")

### 4. Run Chatbot API

```bash
python chatbot_api.py
```

**Expected Output:**
```
 * Running on http://127.0.0.1:5050
 * Debug mode: off
WARNING in app.run(): This is a development server. Do not use it in production.
```

✅ API is now running on `http://localhost:5050`

### 5. Test the API

```bash
# Using curl
curl -X POST http://localhost:5050/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "I have no internet connection"}'

# Expected response:
# {"response": "I can help with your internet connection issue..."}
```

## 📐 Project Structure

```
chatbot/
├── app.py                           # Streamlit UI (optional)
├── chatbot_api.py                   # Flask API server
├── admin_agent.py                   # Main agent orchestration
├── agent_test.py                    # Testing script
├── requirements.txt                 # Python dependencies
├── .env                             # Configuration (create this)
├── .env.example                     # Configuration template
├── agents/
│   ├── classifier_agent.py          # Intent classification agent
│   └── knoweledge_agent.py          # Knowledge base retrieval agent
├── tools/
│   └── knoweledge_base_tool.py      # Vector search implementation
├── utility/
│   ├── llm_config.py                # Azure OpenAI configuration
│   ├── prompt.py                    # Agent instructions & prompts
│   └── __pycache__/
├── data/
│   ├── knoweledge_base.json         # Historical ticket solutions (284 entries)
│   └── dummy_tickets.csv            # Training/test data
├── embeding/
│   └── build_index.py               # FAISS index builder
└── uploads/                         # File upload directory
```

## 🔌 API Endpoints

### Chat Endpoint
```
POST /chat
Content-Type: application/json

Request:
{
  "message": "I have no internet connection"
}

Response:
{
  "response": "AI agent response with solution or suggestion",
  "confidence": 0.95,
  "category": "No Internet"
}
```

### Health Check (Optional)
```
GET /health

Response:
{
  "status": "ok",
  "timestamp": "2026-01-17T10:30:45Z"
}
```

## 🤖 Agents Overview

### Classifier Agent
**Purpose**: Intent detection and ticket categorization

**Input**: User's problem description
```
"My WiFi is not working"
```

**Output**: JSON with classification
```json
{
  "description": "WiFi not working",
  "category": "Wi-Fi Configuration Issue",
  "confidence": 0.92
}
```

**Categories**:
- No Internet
- Slow Internet Speed
- Router / ONT Issue
- Wi-Fi Configuration Issue
- Network Outage
- Slow Performance
- Authentication Issue
- Hardware Failure
- Application Bug
- Change Request
- Access Request
- Billing / Account
- Other

### Knowledge Base Agent
**Purpose**: Retrieve relevant historical solutions

**Process**:
1. Receives user's problem description
2. Converts text to embedding (Sentence-Transformers)
3. Searches FAISS vector index
4. Returns top 3 matching historical solutions
5. Presents solutions with relevance scores

**Example**:
```
Input: "Router not turning on"
↓
Output: [
  {
    "id": "121",
    "problem": "Router power light is off",
    "solution": "Check power adapter connection and outlet",
    "similarity": 0.98
  },
  {
    "id": "45",
    "problem": "Device not powering up",
    "solution": "Try different power outlet",
    "similarity": 0.87
  }
]
```

### Manager Agent (Orchestrator)
**Purpose**: Coordinate agents and manage conversation

**Process**:
1. Receives user message
2. Routes to Classifier Agent
3. Routes to Knowledge Base Agent
4. Aggregates responses
5. Presents consolidated answer
6. Manages max 6 conversation rounds

## 📊 Knowledge Base

**Location**: `data/knoweledge_base.json`

**Format**:
```json
{
  "tickets": [
    {
      "id": "121",
      "category": "No Internet",
      "problem": "Router power light is off",
      "solution": "Plugged the power adapter firmly into wall socket; service restored"
    },
    {
      "id": "122",
      "category": "Wi-Fi Configuration Issue",
      "problem": "User forgot their Wi-Fi password",
      "solution": "Reset WPA2 Pre-Shared Key at 192.168.1.1"
    }
  ]
}
```

**Contains**: 284 ticket solutions with categories and resolutions

### Add New Solutions

1. Edit `data/knoweledge_base.json`
2. Add new entry with `id`, `category`, `problem`, `solution`
3. Rebuild FAISS index:
   ```bash
   python embeding/build_index.py
   ```

## 🛠️ Configuration

### Environment Variables (.env)

```
# Azure OpenAI Configuration
AZURE_OPENAI_API_KEY=<your-api-key>
AZURE_OPENAI_ENDPOINT=<your-endpoint>
AZURE_DEPLOYMENT_NAME=<deployment-name>
AZURE_API_VERSION=2024-02-15-preview

# Azure Search Configuration (optional)
AZURE_SEARCH_ENDPOINT=<search-endpoint>
AZURE_SEARCH_KEY=<search-key>
AZURE_SEARCH_INDEX_NAME=it-ticket-solutions-index

# Agent Configuration
MAX_CONVERSATION_ROUNDS=6
TEMPERATURE=0.0
```

### Agent Prompts

Edit `utility/prompt.py` to customize agent behavior:

```python
# Classifier Agent System Prompt
CLASSIFIER_SYSTEM_PROMPT = """You are a technical support ticket classifier...
Classify the user's issue into one of these categories: [list of categories]"""

# Knowledge Base Agent System Prompt
KNOWLEDGE_SYSTEM_PROMPT = """You are a knowledge base search assistant...
Find and present relevant solutions from our historical database."""
```

## 🧪 Testing

### Test API Endpoint

```bash
# Using Python requests
python -c "
import requests
response = requests.post(
    'http://localhost:5050/chat',
    json={'message': 'Internet is down'}
)
print(response.json())
"
```

### Run Agent Tests

```bash
python agent_test.py
```

### Manual Testing Script

Create `test_chatbot.py`:
```python
import requests
import json

def test_chat():
    messages = [
        "I have no internet",
        "WiFi password forgot",
        "Router is off"
    ]
    
    for msg in messages:
        response = requests.post(
            'http://localhost:5050/chat',
            json={'message': msg}
        )
        print(f"User: {msg}")
        print(f"Bot: {response.json()['response']}\n")

if __name__ == "__main__":
    test_chat()
```

Run:
```bash
python test_chatbot.py
```

## 🎨 Optional: Streamlit Web UI

### Run Streamlit App

```bash
python -m streamlit run app.py
```

Opens at `http://localhost:8501`

**Features**:
- Chat interface
- Conversation history
- Feedback collection
- Solution rating

## 🛠️ Common Tasks

### Rebuild FAISS Index

After updating knowledge base:
```bash
python embeding/build_index.py
```

This rebuilds the vector index for semantic search.

### Check Dependencies

```bash
pip list
```

### Update Dependencies

```bash
pip install --upgrade -r requirements.txt
```

### Create Production Requirements

```bash
pip freeze > requirements.txt
```

## 🐛 Troubleshooting

### Error: "ModuleNotFoundError: No module named 'autogen'"
**Problem**: Dependencies not installed
**Solution**:
```bash
pip install -r requirements.txt
```

### Error: "Azure OpenAI API key not found"
**Problem**: .env file not configured
**Solution**:
1. Create `.env` in chatbot folder
2. Add `AZURE_OPENAI_API_KEY=your-key`
3. Restart API: `python chatbot_api.py`

### Error: "FAISS index not found"
**Problem**: Vector index not built
**Solution**:
```bash
python embeding/build_index.py
```

### Error: "Port 5050 already in use"
**Problem**: Another process uses port 5050
**Solution** (Windows):
```bash
netstat -ano | findstr 5050
taskkill /PID <PID> /F

# Or change port in chatbot_api.py
app.run(port=5051)
```

### Error: "Azure OpenAI rate limit exceeded"
**Problem**: Too many API requests
**Solution**:
1. Wait a few minutes
2. Check Azure OpenAI pricing tier
3. Implement request throttling in code

### Slow Response Time
**Problem**: Large knowledge base or network latency
**Solution**:
1. Check Azure OpenAI status
2. Reduce knowledge base size
3. Optimize FAISS search parameters
4. Use local LLM alternative

## 📦 Dependencies

Key packages:
- `autogen` - Multi-agent framework
- `flask` - REST API server
- `sentence-transformers` - Text embeddings
- `faiss-cpu` - Vector similarity search
- `openai` - Azure OpenAI client
- `pydantic` - Data validation
- `python-dotenv` - Environment variables
- `streamlit` - Optional web UI

## 📚 Integration with Frontend

The frontend connects to this chatbot API:

```javascript
// Frontend code
const response = await fetch('http://localhost:5050/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: userMessage })
});
const data = await response.json();
console.log(data.response);
```

**Ensure**:
- Chatbot API running on `localhost:5050`
- CORS enabled in Flask (already configured)
- Frontend `.env` has correct chatbot URL

## 🚢 Production Deployment

### Before Deploying:
1. ✅ Use strong Azure API keys
2. ✅ Set `TEMPERATURE=0` for consistent responses
3. ✅ Optimize FAISS index for production data
4. ✅ Set up error logging
5. ✅ Configure rate limiting
6. ✅ Use Gunicorn instead of Flask dev server

### Deploy with Gunicorn

```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5050 chatbot_api:app
```

### Docker Deployment

Create `Dockerfile`:
```dockerfile
FROM python:3.9-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .
CMD ["python", "chatbot_api.py"]
```

Build and run:
```bash
docker build -t cortexdesk-chatbot .
docker run -p 5050:5050 --env-file .env cortexdesk-chatbot
```

## ✅ Checklist for Running Locally

- [ ] Python 3.8+ installed
- [ ] Virtual environment created and activated
- [ ] `pip install -r requirements.txt` completed
- [ ] `.env` file created with Azure OpenAI keys
- [ ] `python chatbot_api.py` starts without errors
- [ ] `http://localhost:5050/chat` responds to POST requests
- [ ] Can chat with bot and get responses
- [ ] Knowledge base has solutions (284 entries)
- [ ] FAISS index exists in `data/` folder

---

**Last Updated**: January 2026 | Chatbot Service | Python 3.8+
