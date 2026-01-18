import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { login } from "../services/AuthApi";
import { saveUserSession, getCurrentUser } from "../auth/auth";

export default function Login({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  
  const mutation = useMutation({
    mutationFn: (creds) => login(creds),
    onSuccess: (data) => {
      saveUserSession(data);
      onLogin(getCurrentUser());
    },
  });

  const handleLogin = (e) => {
    e.preventDefault();
    if (username.trim() && password.trim()) {
      mutation.mutate({ username, password });
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && username.trim() && password.trim()) {
      handleLogin(e);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1 className="login-title">CortexDesk</h1>
          <p className="login-subtitle">Network Ticketing System</p>
        </div>

        <form className="login-form-container" onSubmit={handleLogin}>
          <p className="login-description">Enter your credentials to access CortexDesk</p>

          <div className="form-group">
            <label className="form-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              Username
            </label>
            <input 
              type="text"
              className="form-control"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={mutation.isPending}
              autoFocus
            />
          </div>

          <div className="form-group">
            <div className="form-label-row">
              <label className="form-label">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
                Password
              </label>
              <a href="#" className="form-link-small">Forgot?</a>
            </div>
            <div className="password-input-wrapper">
              <input 
                type={showPassword ? "text" : "password"}
                className="form-control"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyPress={handleKeyPress}
                disabled={mutation.isPending}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                title={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-4-11-4s1.6-2.1 4.3-4m7.08-7.08A10.07 10.07 0 0 1 12 4c7 0 11 4 11 4s-1.6 2.1-4.3 4"></path>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                  </svg>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                )}
              </button>
            </div>
          </div>

          {mutation.isError && (
            <div className="alert alert-danger alert-login" role="alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {mutation.error?.message || "Invalid username or password"}
            </div>
          )}

          <button 
            type="submit" 
            className="btn btn-primary w-100"
            disabled={mutation.isPending || !username.trim() || !password.trim()}
          >
            {mutation.isPending ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Signing In...
              </>
            ) : "Sign In"}
          </button>
        </form>

        <div className="login-footer">
          <p className="login-footer-text">Welcome to CortexDesk</p>
        </div>
      </div>
    </div>
  );
}
