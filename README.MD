# CortexDesk - Network Ticketing System

A comprehensive IT support ticketing platform with AI-powered chatbot assistance, built for telecom companies to manage network support requests efficiently.

## 🎯 Overview

CortexDesk is an enterprise-grade ticketing system designed to:
- **Reduce Support Load**: AI-powered chatbot handles common issues before ticket creation
- **Improve First-Contact Resolution**: Intelligent agent suggests solutions from historical data
- **Track SLAs**: Automated SLA management and deadline tracking
- **Role-Based Access**: Separate dashboards for customers, engineers, managers, and admins

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    CORTEXDESK PROJECT                         │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────────┐    ┌─────────────────┐    ┌────────────┐
│  │  React Frontend │    │ Spring Boot API │    │   Python   │
│  │  (Port 5173)    │◄──►│  (Port 9091)    │◄──►│  Chatbot   │
│  │                 │    │                 │    │ (Port 5050)│
│  └─────────────────┘    └────────┬────────┘    └────────────┘
│                                  │
│                            ┌──────▼──────┐
│                            │    MySQL    │
│                            │  (Port 3306)│
│                            └─────────────┘
│
│  ┌─────────────────────────────────────────────┐
│  │      Azure OpenAI + FAISS Vector DB        │
│  │  (For AI diagnostics & knowledge base)      │
│  └─────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────┘
```

## 📚 Project Structure

```
CortexDesk/
├── backend/               # Spring Boot REST API (Java 17, MySQL)
├── frontend/              # React + Vite web application (Node.js)
├── chatbot/               # Python multi-agent system (AutoGen + Flask)
└── README.md              # This file
```

## 🚀 Quick Start (Local Development)

### Prerequisites
- **Java 17+** (for backend)
- **Node.js 16+** (for frontend)
- **Python 3.8+** (for chatbot)
- **MySQL 5.7+** (for database)

### 1️⃣ Backend (Spring Boot)
```bash
cd backend
./mvnw clean install
# Configure MySQL and credentials in src/main/resources/application.yml
./mvnw spring-boot:run
# Backend running on http://localhost:9091
```

### 2️⃣ Frontend (React)
```bash
cd frontend
npm install
npm run dev
# Frontend running on http://localhost:5173
```

### 3️⃣ Chatbot (Python)
```bash
cd chatbot
pip install -r requirements.txt
# Configure .env with Azure OpenAI credentials
python chatbot_api.py
# Chatbot API running on http://localhost:5050
```

> **Note**: See detailed README.md in each folder for full setup instructions

## 🎯 Core Features

### Backend (Spring Boot)
- ✅ Ticket lifecycle management (NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED)
- ✅ Role-based access control (Customer, Engineer, Manager, Admin)
- ✅ SLA tracking with automated alerts
- ✅ AI-powered resolution suggestions via Google Vertex AI
- ✅ Email notifications (Gmail SMTP)
- ✅ JWT authentication
- ✅ File attachment support

### Frontend (React)
- ✅ Role-based dashboards for each user type
- ✅ Ticket creation and lifecycle management
- ✅ Embedded AI chatbot for customers
- ✅ Real-time SLA status visualization
- ✅ Priority and status badges
- ✅ Form validation with React Hook Form + Yup
- ✅ Data caching with TanStack React Query

### Chatbot (Python)
- ✅ Automatic intent classification (13+ categories)
- ✅ Semantic search through historical solutions
- ✅ Multi-agent orchestration with AutoGen
- ✅ Azure OpenAI integration
- ✅ FAISS vector database for fast similarity search
- ✅ REST API endpoint for frontend integration

## 🔗 Default Credentials (Local Dev Only)

| Component | Username | Password | Notes |
|-----------|----------|----------|-------|
| MySQL | root | root | Change in `application.yml` |
| Backend Admin | admin@cortexdesk.com | password | Seeded automatically |
| Backend User | user@cortexdesk.com | password | Seeded automatically |

⚠️ **Security Warning**: Change these credentials before deploying to production!

## 📡 API Endpoints

### Authentication
```
POST /api/auth/login
```

### Tickets
```
POST /api/tickets                    # Create ticket
GET  /api/tickets                    # List tickets
GET  /api/tickets/{id}              # Get ticket details
POST /api/tickets/assign             # Assign to engineer
POST /api/tickets/pick              # Pick up ticket
POST /api/tickets/resolve           # Mark resolved
POST /api/tickets/close             # Close ticket
```

### Issue Categories
```
GET  /api/issue-categories          # List categories
POST /api/issue-categories          # Create category (admin)
```

### Chatbot
```
POST /chat                           # Send message to chatbot
```

See individual component README files for full API documentation.

## 🛠️ Development Workflow

### Working on Backend
```bash
cd backend
./mvnw clean install        # Build
./mvnw spring-boot:run      # Run
./mvnw test                 # Run tests
```

### Working on Frontend
```bash
cd frontend
npm install                 # Install dependencies
npm run dev                 # Start dev server with hot reload
npm run build              # Production build
npm run lint               # Check for lint errors
```

### Working on Chatbot
```bash
cd chatbot
pip install -r requirements.txt     # Install dependencies
python chatbot_api.py               # Run Flask API
python -m streamlit run app.py      # Run Streamlit UI (optional)
```

## 🔧 Configuration Files

| Component | Config File | Purpose |
|-----------|------------|---------|
| Backend | `src/main/resources/application.yml` | Database, mail, server config |
| Frontend | `.env.local` (create if needed) | API URL, chatbot URL |
| Chatbot | `.env` | Azure OpenAI credentials |

## 🗄️ Database Setup

### For MySQL:
```sql
CREATE DATABASE networkticketingdb;
-- Tables are auto-created by Hibernate (ddl-auto: update)
-- Initial data seeded by UserSeeder and IssueCategorySeeder
```

The backend automatically seeds:
- Default users (admin, customer, engineer, manager)
- Issue categories (No Internet, Slow Speed, Router Issues, etc.)

## 📝 Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Frontend** | React | 19.2.0 |
|  | Vite | 7.2.4 |
|  | Bootstrap | 5.3.0 |
|  | React Router | 7.11.0 |
|  | Axios | 1.4.0 |
| **Backend** | Spring Boot | 3.5.9 |
|  | Java | 17 |
|  | MySQL | 5.7+ |
|  | JPA/Hibernate | Latest |
| **Chatbot** | Python | 3.8+ |
|  | AutoGen | Latest |
|  | Flask | Latest |
|  | FAISS | CPU |
|  | Azure OpenAI | Latest |

## 📖 Documentation

- [Backend README](backend/README.md) - Spring Boot setup and API details
- [Frontend README](frontend/README.md) - React development guide
- [Chatbot README](chatbot/README.md) - Python chatbot setup

## 🐛 Troubleshooting

### MySQL Connection Failed
- Ensure MySQL is running: `mysql -u root -p`
- Check credentials in `backend/src/main/resources/application.yml`
- Default: username=root, password=root

### Port Already in Use
- Backend (9091): `netstat -ano | findstr 9091` and kill the process
- Frontend (5173): `npm run dev` uses next available port automatically
- Chatbot (5050): Change port in `chatbot_api.py`

### Frontend Can't Connect to Backend
- Ensure backend is running on `http://localhost:9091`
- Check CORS configuration in `backend/SecurityConfig.java`
- Frontend should be running on `http://localhost:5173`

### Python Dependencies Issue
```bash
# Clear and reinstall
pip uninstall -r requirements.txt -y
pip install -r requirements.txt
```

## 💡 Next Steps

1. **Clone/Setup**: Follow Quick Start above
2. **Database**: Set up MySQL with credentials
3. **Configuration**: Update `application.yml` and chatbot `.env`
4. **Run Locally**: Start all three components
5. **Test**: Access frontend at http://localhost:5173
6. **Login**: Use default credentials (see above)

## 📞 Support

For issues specific to each component, see:
- Backend issues → [backend/README.md](backend/README.md)
- Frontend issues → [frontend/README.md](frontend/README.md)
- Chatbot issues → [chatbot/README.md](chatbot/README.md)

## 📄 License

Internal project - Prodapt

---

**Last Updated**: January 2026 | **Status**: Active Development
