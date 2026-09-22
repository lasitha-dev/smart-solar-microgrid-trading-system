/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: 404 Not Found fallback view.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, Home } from 'lucide-react';

export const NotFoundPage = () => {
  return (
    <div style={{
      minHeight: '80vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: '2rem'
    }}>
      <div style={{
        width: '72px',
        height: '72px',
        borderRadius: '50%',
        background: 'var(--status-pending-bg)',
        color: 'var(--status-pending)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '1.5rem'
      }}>
        <AlertTriangle size={36} />
      </div>
      <h1 style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>404 - Page Not Found</h1>
      <p style={{ color: 'var(--text-muted)', maxWidth: '400px', marginBottom: '2rem' }}>
        The administrative view or resource you requested does not exist in the microgrid portal.
      </p>
      <Link to="/pending-approvals" className="btn btn-primary">
        <Home size={18} />
        <span>Return to Dashboard</span>
      </Link>
    </div>
  );
};
