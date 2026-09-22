/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Dedicated administrative authentication screen for Backoffice officers and Administrators.
 */

import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Sun, Shield, Lock, User, Eye, EyeOff, AlertCircle, CheckCircle2 } from 'lucide-react';

export const LoginPage = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/pending-approvals';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!identifier.trim()) {
      setError('Please enter your Username or NIC.');
      return;
    }

    if (!password) {
      setError('Please enter your password.');
      return;
    }

    setLoading(true);
    try {
      const userData = await login(identifier, password);
      // Route Grid Operator to Profile or requested page; Backoffice to Pending Approvals or requested page
      if (userData.role === 'GridOperator') {
        navigate(from && from !== '/pending-approvals' ? from : '/profile', { replace: true });
      } else {
        navigate(from, { replace: true });
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'radial-gradient(circle at top right, rgba(245, 158, 11, 0.12), transparent 45%), radial-gradient(circle at bottom left, rgba(6, 182, 212, 0.08), transparent 45%), #090d16',
      padding: '1.5rem',
    }}>
      <div style={{
        maxWidth: '440px',
        width: '100%',
        background: 'var(--bg-card)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-lg), 0 0 35px rgba(245, 158, 11, 0.15)',
        overflow: 'hidden'
      }}>
        {/* Brand Header Banner */}
        <div style={{
          padding: '2.5rem 2rem 1.5rem',
          textAlign: 'center',
          background: 'radial-gradient(circle at top, rgba(245, 158, 11, 0.1), transparent 70%), var(--bg-card-header)',
          borderBottom: '1px solid var(--border-subtle)'
        }}>
          <div style={{
            width: '56px',
            height: '56px',
            background: 'linear-gradient(135deg, #f59e0b, #d97706)',
            color: '#0b1120',
            borderRadius: 'var(--radius-md)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1rem',
            boxShadow: 'var(--shadow-glow)'
          }}>
            <Sun size={32} />
          </div>

          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.25rem' }}>Smart Solar Staff Portal</h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Microgrid Operations, Trading & User Lifecycle
          </p>

          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            background: 'rgba(245, 158, 11, 0.12)',
            color: 'var(--solar-amber)',
            padding: '0.25rem 0.75rem',
            borderRadius: 'var(--radius-full)',
            fontSize: '0.75rem',
            fontWeight: 700,
            marginTop: '0.75rem',
            border: '1px solid rgba(245, 158, 11, 0.3)'
          }}>
            <Shield size={13} />
            <span>AUTHORIZED STAFF ACCESS</span>
          </div>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} style={{ padding: '2rem' }}>
          {error && (
            <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Username or NIC</label>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. staff_officer or NIC"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                style={{ paddingLeft: '2.5rem' }}
                autoFocus
                required
              />
              <User size={18} style={{
                position: 'absolute',
                left: '0.85rem',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-dim)'
              }} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Secure Password</label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                className="form-control"
                placeholder="Enter account password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                style={{ paddingLeft: '2.5rem', paddingRight: '2.5rem' }}
                required
              />
              <Lock size={18} style={{
                position: 'absolute',
                left: '0.85rem',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-dim)'
              }} />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '0.75rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-dim)',
                  cursor: 'pointer',
                  display: 'flex'
                }}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '0.75rem', marginTop: '0.5rem' }}
            disabled={loading}
          >
            {loading ? <span className="spinner"></span> : <Shield size={18} />}
            <span>{loading ? 'Authenticating...' : 'Sign In to Portal'}</span>
          </button>
        </form>
      </div>
    </div>
  );
};
