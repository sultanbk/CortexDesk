# Frontend - Network Ticketing Web UI (React + Vite)

Modern, responsive web interface for CortexDesk ticketing system. Built with React 19, Vite, and Bootstrap 5, featuring role-based dashboards and embedded AI chatbot.

## 🎯 Overview

This frontend application provides:
- Role-based dashboards (Customer, Engineer, Manager, Admin)
- Ticket creation and management interface
- Real-time SLA status visualization
- Embedded AI chatbot for customers
- Form validation and error handling
- JWT authentication
- Responsive design (mobile & desktop)

## 📋 Prerequisites

### System Requirements
- **Node.js 16.0.0 or higher**
- **npm 7.0.0 or higher** (or yarn/pnpm)
- **Git** (for cloning)

### Verify Installation
```bash
node --version     # Should show v16.0.0+
npm --version      # Should show 7.0.0+
```

## 🚀 Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
```

> This may take a minute to download all packages (~400MB)

### 2. Run Development Server

```bash
npm run dev
```

**Expected Output:**
```
  VITE v7.2.4  ready in 234 ms

  ➜  Local:   http://localhost:5173/
  ➜  Press h to show help
```

✅ Open **`http://localhost:5173`** in your browser

### 3. Login with Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Customer | customer@cortexdesk.com | password |
| Manager | manager@cortexdesk.com | password |
| Engineer | engineer@cortexdesk.com | password |
| Admin | admin@cortexdesk.com | password |

> **Note**: Ensure backend is running on `http://localhost:9091`

## 📐 Project Structure

```
frontend/src/
├── main.jsx                     # App entry point
├── App.jsx                      # Root component & routing
├── ErrorBoudry.jsx              # Error boundary wrapper
├── index.css                    # Global styles
├── queryClient.js               # React Query config
├── auth/
│   └── auth.js                  # Auth utilities, JWT management
├── pages/                       # Page components
│   ├── Login.jsx
│   ├── CreateTicket.jsx
│   ├── TicketList.jsx
│   ├── DashboardCustomer.jsx
│   ├── DashboardManager.jsx
│   ├── DashboardEngineer.jsx
│   ├── AdminIssueCategories.jsx
│   └── EngineerQueue.jsx
├── components/                  # Reusable components
│   ├── ChatLauncher.jsx         # AI chatbot widget
│   ├── AdminGuard.jsx           # Admin role protection
│   ├── PriorityBadge.jsx        # Priority display
│   ├── StatusBadge.jsx          # Status display
│   └── ToastProvider.jsx        # Toast notifications
├── services/                    # API integration
│   ├── axiosClient.js           # Axios instance with JWT
│   ├── authApi.js               # Auth endpoints
│   ├── ticketApi.js             # Ticket endpoints
│   ├── issueCategoryApi.js      # Category endpoints
│   └── chatbotApi.js            # Chatbot endpoints
├── assets/                      # Images & static files
└── styles/                      # CSS modules
```

## 🔧 Configuration

### Backend API URL

By default, the app connects to `http://localhost:9091`.

**To change backend URL**, edit `src/services/axiosClient.js`:
```javascript
const API_BASE_URL = process.env.VITE_API_URL || 'http://localhost:9091/api';
```

Or create `.env.local`:
```
VITE_API_URL=http://your-api-url:9091/api
VITE_CHATBOT_URL=http://localhost:5050
```

### Chatbot Configuration

By default, the chatbot runs on `http://localhost:5050`.

**To change chatbot URL**, edit `src/services/chatbotApi.js` or `.env.local`:
```
VITE_CHATBOT_URL=http://your-chatbot-url:5050
```

## 🏃 Available Scripts

```bash
# Development server with hot reload
npm run dev

# Build for production
npm run build

# Preview production build locally
npm run preview

# Run ESLint to check code quality
npm run lint

# Fix ESLint errors automatically
npm run lint -- --fix
```

## 🎨 Pages Overview

### Login Page
- JWT authentication
- Email/password input
- Error handling
- "Remember me" option

### Customer Dashboard
- List of own tickets
- Quick ticket statistics
- "Create New Ticket" button
- Embedded AI chatbot widget
- Ticket detail modal

### Manager Dashboard
- View all tickets
- Assign tickets to engineers
- Filter by status/priority
- SLA tracking
- Performance metrics

### Engineer Dashboard
- View assigned tickets
- "Pick up" ticket to start work
- Ticket details and history
- Resolution input
- Mark as resolved

### Admin Dashboard
- Manage issue categories
- Create/edit/delete categories
- View category statistics

## 🤖 Chatbot Integration

The ChatLauncher component provides an embedded AI chatbot:

```jsx
import ChatLauncher from './components/ChatLauncher';

function CustomerDashboard() {
  return (
    <div>
      {/* ... dashboard content ... */}
      <ChatLauncher />  {/* Floating chatbot widget */}
    </div>
  );
}
```

**Features:**
- Floating widget in bottom-right corner
- Minimizable/maximizable
- Real-time conversation
- Ticket escalation from chat
- Session history

## 🔐 Authentication Flow

1. User enters credentials on Login page
2. Frontend sends POST to `/api/auth/login`
3. Backend returns JWT token
4. Token stored in `localStorage`
5. Axios interceptor adds token to all requests
6. Token refreshed on app startup
7. Expired token triggers re-login

**JWT Storage:**
```javascript
localStorage.setItem('authToken', token);
localStorage.setItem('user', JSON.stringify(user));
```

**Token in Requests:**
```javascript
// Automatically added by axios interceptor
Authorization: Bearer <token>
```

## 📦 Dependencies

Key dependencies (see `package.json`):

| Package | Version | Purpose |
|---------|---------|---------|
| react | 19.2.0 | UI framework |
| react-dom | 19.2.0 | DOM rendering |
| react-router-dom | 7.11.0 | Client routing |
| @tanstack/react-query | 5.0.0 | Data fetching |
| axios | 1.4.0 | HTTP client |
| react-hook-form | 7.45.1 | Form state |
| yup | 1.2.0 | Validation |
| bootstrap | 5.3.0 | CSS framework |

## 🧪 Testing

Currently, unit tests are not configured. To add:

```bash
npm install --save-dev vitest @testing-library/react @testing-library/jest-dom
```

Then create test files:
```
src/components/__tests__/
src/pages/__tests__/
src/services/__tests__/
```

Run tests:
```bash
npm run test
```

## 🛠️ Common Tasks

### Add a New Page

1. Create page component in `src/pages/NewPage.jsx`
2. Import in `App.jsx`
3. Add route:
```jsx
<Route path="/new-page" element={<NewPage />} />
```
4. Add navigation link

### Add a New Component

1. Create in `src/components/NewComponent.jsx`
2. Import and use in pages
3. Keep components reusable and prop-based

### Call Backend API

```javascript
import { axiosClient } from '../services/axiosClient';

// GET request
const { data } = await axiosClient.get('/tickets');

// POST request
const { data } = await axiosClient.post('/tickets', {
  title: 'Issue',
  description: 'Description'
});
```

### Use React Query for Data Fetching

```javascript
import { useQuery } from '@tanstack/react-query';
import { axiosClient } from '../services/axiosClient';

function TicketList() {
  const { data: tickets, isLoading, error } = useQuery({
    queryKey: ['tickets'],
    queryFn: async () => {
      const { data } = await axiosClient.get('/tickets');
      return data;
    }
  });

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <ul>
      {tickets.map(ticket => (
        <li key={ticket.id}>{ticket.title}</li>
      ))}
    </ul>
  );
}
```

### Form Validation with React Hook Form + Yup

```javascript
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';

const schema = yup.object({
  email: yup.string().email().required(),
  password: yup.string().min(6).required()
});

function LoginForm() {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(schema)
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} />
      {errors.email && <p>{errors.email.message}</p>}
    </form>
  );
}
```

## 🐛 Troubleshooting

### Error: "Cannot find module"
**Problem**: Dependencies not installed
**Solution**:
```bash
rm -rf node_modules package-lock.json
npm install
```

### Error: "Port 5173 already in use"
**Problem**: Another process uses port 5173
**Solution** (Windows):
```bash
# Find and kill process
netstat -ano | findstr 5173
taskkill /PID <PID> /F

# Or Vite will use next available port
```

### Error: "Cannot reach backend"
**Problem**: Backend not running or wrong URL
**Solution**:
1. Ensure backend running: `http://localhost:9091`
2. Check `axiosClient.js` API URL
3. Check CORS enabled in backend
4. Open browser console (F12) for detailed error

### Error: "401 Unauthorized"
**Problem**: Invalid or expired JWT token
**Solution**:
```bash
# Clear localStorage
localStorage.clear()

# Re-login
# Refresh page
```

### Error: "CORS error"
**Problem**: Backend CORS not configured for frontend
**Solution**: Edit backend `SecurityConfig.java`:
```java
@CrossOrigin(origins = "http://localhost:5173")
```

### Page Loads But No Data
**Problem**: Network request failing silently
**Solution**:
1. Open DevTools (F12)
2. Check Network tab for failed requests
3. Check Console for JavaScript errors
4. Verify token is in localStorage

## 🎨 Styling

### Bootstrap Classes
The app uses Bootstrap 5 for styling:
```jsx
<div className="container">
  <div className="row">
    <div className="col-md-6">
      <button className="btn btn-primary">Click Me</button>
    </div>
  </div>
</div>
```

### Custom CSS
Global styles in `src/index.css`, component-specific in `src/styles/`

### Theme Customization
Bootstrap variables can be overridden before importing Bootstrap:
```css
/* index.css */
:root {
  --bs-primary: #007bff;
  --bs-secondary: #6c757d;
}

@import 'bootstrap/scss/bootstrap';
```

## 📱 Responsive Design

The app uses Bootstrap's responsive grid:
- Mobile-first approach
- Breakpoints: xs, sm, md, lg, xl, xxl
- Flexbox utilities for alignment
- Responsive images

## 🚢 Production Build

### Build for Production
```bash
npm run build
```

Output: `dist/` directory

### Preview Production Build
```bash
npm run preview
```

### Deploy to Server
```bash
# Copy dist folder to web server
# Configure server for SPA routing:
# All routes → index.html
```

**Nginx Example:**
```nginx
location / {
  try_files $uri /index.html;
}
```

## ✅ Checklist for Running Locally

- [ ] Node.js 16+ installed
- [ ] `npm install` completed
- [ ] Backend running on localhost:9091
- [ ] `npm run dev` starts without errors
- [ ] `http://localhost:5173` loads
- [ ] Can login with default credentials
- [ ] Chatbot endpoint configured (if using)
- [ ] No CORS errors in console

---

**Last Updated**: January 2026 | Frontend UI | React 19.2.0 + Vite 7.2.4
