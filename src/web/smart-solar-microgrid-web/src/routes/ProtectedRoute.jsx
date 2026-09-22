/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Route guard component enforcing authentication and Backoffice role authorization.
 */

import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Sun } from 'lucide-react';

export const ProtectedRoute = ({ children }) => {
  const { user, loading, isBackoffice } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--bg-main)',
        color: 'var(--solar-amber)',
        gap: '1rem'
      }}>
        <Sun className="animate-spin" size={48} />
        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem' }}>Verifying Administrator Session...</p>
      </div>
    );
  }

  if (!user || !isBackoffice) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};
