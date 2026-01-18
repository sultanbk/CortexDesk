import { Routes, Route, useNavigate, Navigate } from "react-router-dom";
import { useState, useEffect, useRef } from "react";
import CreateTicket from "./pages/CreateTicket";
import TicketList from "./pages/TicketList";
import Login from "./pages/Login";
import DashboardCustomer from "./pages/DashboardCustomer";
import DashboardManager from "./pages/DashboardManager";
import DashboardEngineer from "./pages/DashboardEngineer";
import AdminIssueCategories from "./pages/AdminIssueCategories";
import AdminGuard from "./components/AdminGuard";
import { getCurrentUser, logout } from "./auth/auth";
import ErrorBoundary from "./ErrorBoudry";

import ChatLauncher from "./components/ChatLauncher";

export default function App() {
  const [user, setUser] = useState(getCurrentUser());
  const navigate = useNavigate();

  const roleKey = (user?.role || '').toString().toUpperCase();
  const prevUserRef = useRef(getCurrentUser());

  useEffect(() => {
    // only navigate automatically when user transitions from not-logged-in -> logged-in
    const prev = prevUserRef.current;
    if (!prev && user) {
      if (roleKey.includes('CUSTOMER')) navigate('/dashboard/customer');
      else if (roleKey.includes('MANAGER')) navigate('/dashboard/manager');
      else if (roleKey.includes('ENGINEER')) navigate('/dashboard/engineer');
    }
    prevUserRef.current = user;
  }, [user, navigate, roleKey]);

  if (!user) return <Login onLogin={setUser} />;

  return (
    <>
      <nav className="navbar navbar-dark bg-dark">
        <div className="container-fluid">
          <div className="navbar-brand-group">
            <span className="navbar-brand mb-0 h1">CortexDesk</span>
          </div>
          <div className="d-flex gap-2">
            {roleKey.includes("CUSTOMER") && <button className="btn btn-sm btn-outline-light" onClick={() => navigate("/create")}>Create Ticket</button>}
            {roleKey.includes("CUSTOMER") && <button className="btn btn-sm btn-outline-light" onClick={() => navigate("/dashboard/customer")}>Customer Dashboard</button>}
            {roleKey.includes("MANAGER") && <button className="btn btn-sm btn-outline-light" onClick={() => navigate("/dashboard/manager")}>Manager Dashboard</button>}
            {roleKey.includes("ENGINEER") && <button className="btn btn-sm btn-outline-light" onClick={() => navigate("/dashboard/engineer")}>Engineer Dashboard</button>}
            {roleKey.includes("ADMIN") && <button className="btn btn-sm btn-outline-light" onClick={() => navigate("/admin/issue-categories")}>Admin</button>}
            <button className="btn btn-sm btn-outline-danger" onClick={() => { logout(); setUser(null); prevUserRef.current = null; navigate("/"); }}>Logout</button>
          </div>
        </div>
      </nav>

      <ErrorBoundary>
        <Routes>
          {/* default root redirects to role dashboard */}

          <Route path="/" element={user ? <Navigate to={roleKey.includes('MANAGER') ? '/dashboard/manager' : roleKey.includes('ENGINEER') ? '/dashboard/engineer' : '/dashboard/customer'} replace /> : <Login onLogin={setUser} />} />

          {roleKey.includes("CUSTOMER") && (
            <Route path="/create" element={<CreateTicket />} />
          )}

          <Route path="/tickets" element={<TicketList role={user.role} />} />

          <Route path="/dashboard/customer" element={<DashboardCustomer />} />
          <Route path="/dashboard/manager" element={<DashboardManager />} />
          <Route path="/dashboard/engineer" element={<DashboardEngineer />} />
          <Route path="/admin/issue-categories" element={<AdminGuard><AdminIssueCategories /></AdminGuard>} />
        </Routes>
      </ErrorBoundary>

      {/* Render ChatLauncher only for customers */}
      {roleKey.includes("CUSTOMER") && <ChatLauncher />}
    </>
  );
}
