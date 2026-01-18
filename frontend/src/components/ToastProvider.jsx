import React, { createContext, useCallback, useContext, useState, useEffect } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, options = {}) => {
    setToast({ message, type: options.severity || 'info', duration: options.duration || 4000, id: Date.now() });
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), toast.duration);
    return () => clearTimeout(timer);
  }, [toast]);

  const handleClose = useCallback(() => setToast(null), []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {toast && (
        <div 
          style={{
            position: 'fixed',
            bottom: '20px',
            right: '20px',
            zIndex: 9999,
            minWidth: '300px'
          }}
        >
          <div className={`alert alert-${toast.type === 'error' ? 'danger' : toast.type === 'warning' ? 'warning' : toast.type === 'success' ? 'success' : 'info'} alert-dismissible fade show`} role="alert">
            {toast.message}
            <button type="button" className="btn-close" onClick={handleClose} aria-label="Close"></button>
          </div>
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}
