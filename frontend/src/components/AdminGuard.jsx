import React from 'react';
import { Navigate } from 'react-router-dom';
import { getCurrentUser } from '../auth/auth';

export default function AdminGuard({ children }) {
  const user = getCurrentUser();
  const role = (user?.role || '').toString().toUpperCase();
  if (!user || !role.includes('ADMIN')) {
    // not authorized -> redirect to role dashboard or login
    if (!user) return <Navigate to="/" replace />;
    if (role.includes('MANAGER')) return <Navigate to="/dashboard/manager" replace />;
    if (role.includes('ENGINEER')) return <Navigate to="/dashboard/engineer" replace />;
    if (role.includes('CUSTOMER')) return <Navigate to="/dashboard/customer" replace />;
    return <Navigate to="/" replace />;
  }
  return children;
}
