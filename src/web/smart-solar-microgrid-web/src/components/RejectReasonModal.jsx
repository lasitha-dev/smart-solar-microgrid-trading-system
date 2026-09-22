/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Rejection and deactivation remarks modal dialog with confirmation.
 */

import React, { useState } from 'react';
import { X, AlertTriangle, XCircle } from 'lucide-react';

export const RejectReasonModal = ({ isOpen, onClose, onConfirm, targetUser, actionType = 'Reject' }) => {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen || !targetUser) return null;

  const handleConfirm = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onConfirm(targetUser, reason);
      setReason('');
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              background: 'var(--status-deactivated-bg)',
              color: 'var(--status-deactivated)',
              padding: '0.5rem',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <AlertTriangle size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem' }}>{actionType} Application</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Target User: <strong style={{ color: 'var(--text-main)' }}>{targetUser.fullName || targetUser.username}</strong> ({targetUser.nic})
              </p>
            </div>
          </div>
          <button className="btn btn-outline btn-sm" onClick={onClose} style={{ border: 'none', padding: '0.35rem' }}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleConfirm}>
          <div className="card-body">
            <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
              Are you sure you want to {actionType.toLowerCase()} this registration? The prosumer will be prohibited from logging in or booking trading slots until re-evaluated.
            </p>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Rejection / Deactivation Reason (Optional)</label>
              <textarea
                className="form-control"
                rows="3"
                placeholder="e.g. Incomplete solar facility proof or location mismatch..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </div>
          </div>

          <div style={{
            padding: '1rem 1.5rem',
            background: 'var(--bg-card-header)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem'
          }}>
            <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-danger" disabled={loading}>
              {loading ? <span className="spinner"></span> : <XCircle size={18} />}
              <span>{loading ? 'Processing...' : `Confirm ${actionType}`}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
