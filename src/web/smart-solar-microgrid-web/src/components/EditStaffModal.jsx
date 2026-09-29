/*
 * Student Role: Member 4 / Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Operational Management
 * Description: Modal dialog for Backoffice administrators editing existing staff and grid operator profile details.
 */

import React, { useState, useEffect } from 'react';
import { X, Edit3, Shield, AlertCircle, Save } from 'lucide-react';
import { adminService } from '../services/adminService';

export const EditStaffModal = ({ isOpen, onClose, onStaffUpdated, user }) => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    address: '',
    role: 'GridOperator',
    latitude: 6.9271,
    longitude: 79.8612,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      setFormData({
        fullName: user.fullName || '',
        email: user.email || '',
        phone: user.phone || '',
        address: user.address || '',
        role: user.role || 'GridOperator',
        latitude: user.latitude || 6.9271,
        longitude: user.longitude || 79.8612,
      });
      setError('');
    }
  }, [user]);

  if (!isOpen || !user) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!formData.fullName.trim() || !formData.email.trim()) {
      setError('Full Name and Email are required.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        address: formData.address.trim(),
        role: formData.role,
        latitude: parseFloat(formData.latitude) || null,
        longitude: parseFloat(formData.longitude) || null,
      };

      const userId = user.id || user.userId;
      await adminService.updateStaffUser(userId, payload);
      onStaffUpdated();
      onClose();
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.Message ||
        err.message ||
        'Failed to update staff user account.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        style={{ maxWidth: '580px', maxHeight: '92vh', overflowY: 'auto' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="card-header" style={{ position: 'sticky', top: 0, zIndex: 10, background: 'var(--bg-surface)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                background: 'var(--role-operator-bg)',
                color: 'var(--role-operator)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Edit3 size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', margin: 0 }}>Edit Staff Account</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
                Update operator credentials and contact parameters
              </p>
            </div>
          </div>
          <button className="btn btn-outline btn-sm" onClick={onClose} style={{ border: 'none', padding: '0.35rem' }}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="card-body" style={{ padding: '1.25rem' }}>
            {error && (
              <div className="alert alert-danger" style={{ marginBottom: '1.25rem' }}>
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            {/* Read-only Identity Banner */}
            <div
              style={{
                display: 'flex',
                gap: '1rem',
                padding: '0.85rem 1rem',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ flex: 1 }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>NIC Number</span>
                <span style={{ fontWeight: 600, color: 'var(--solar-amber)' }}>{user.nic || 'N/A'}</span>
              </div>
              <div style={{ flex: 1 }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>Username</span>
                <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>{user.username || 'N/A'}</span>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <input
                type="text"
                name="fullName"
                className="form-control"
                value={formData.fullName}
                onChange={handleChange}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Email Address *</label>
                <input
                  type="email"
                  name="email"
                  className="form-control"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Phone Number</label>
                <input
                  type="text"
                  name="phone"
                  className="form-control"
                  value={formData.phone}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">System Role *</label>
              <select
                name="role"
                className="form-control"
                value={formData.role}
                onChange={handleChange}
                required
              >
                <option value="GridOperator">Grid Operator</option>
                <option value="Backoffice">Backoffice Administrator</option>
                <option value="Administrator">System Administrator</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Assigned Station / Facility Address</label>
              <input
                type="text"
                name="address"
                className="form-control"
                value={formData.address}
                onChange={handleChange}
                placeholder="e.g. Colombo Substation, Main Street"
              />
            </div>
          </div>

          <div
            className="card-footer"
            style={{
              padding: '1rem 1.25rem',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '0.75rem',
              borderTop: '1px solid var(--border-color)',
            }}
          >
            <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Save size={16} />
              <span>{loading ? 'Saving Changes...' : 'Save Profile'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
