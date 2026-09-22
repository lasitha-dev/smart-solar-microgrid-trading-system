/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Modal dialog component for Backoffice administrators creating new staff accounts with address input and interactive location map picker.
 */

import React, { useState } from 'react';
import { X, UserPlus, Shield, Check, AlertCircle, Mail, Lock, CheckCircle2, MapPin, Compass } from 'lucide-react';
import { adminService } from '../services/adminService';

const STATION_PRESETS = [
  { name: 'Colombo Grid HQ', lat: 6.9271, lon: 79.8612, address: 'No. 50, Sir Chittampalam A. Gardiner Mawatha, Colombo 02' },
  { name: 'Kandy Substation', lat: 7.2906, lon: 80.6337, address: 'Kandy Grid Substation, Peradeniya Road, Kandy' },
  { name: 'Galle Coastal Hub', lat: 6.0535, lon: 80.2210, address: 'Southern Regional Grid Station, Matara Road, Galle' },
  { name: 'Gampaha Solar Hub', lat: 7.0917, lon: 79.9999, address: 'Western North Grid Center, Yakkala Road, Gampaha' },
  { name: 'Kurunegala Node', lat: 7.4863, lon: 80.3623, address: 'North Western Grid Substation, Dambulla Road, Kurunegala' },
  { name: 'Jaffna Solar Park', lat: 9.6615, lon: 80.0255, address: 'Northern Microgrid Operations, Kandy Road, Jaffna' },
];

export const CreateStaffModal = ({ isOpen, onClose, onStaffCreated }) => {
  const [formData, setFormData] = useState({
    nic: '',
    username: '',
    fullName: '',
    email: '',
    phone: '',
    address: '',
    latitude: 6.9271,
    longitude: 79.8612,
    password: '',
    confirmPassword: '',
    role: 'Backoffice',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleSelectPreset = (preset) => {
    setFormData((prev) => ({
      ...prev,
      latitude: preset.lat,
      longitude: preset.lon,
      address: prev.address.trim() ? prev.address : preset.address
    }));
  };

  // Coordinates calculation for OSM Embed
  const lat = parseFloat(formData.latitude) || 6.9271;
  const lon = parseFloat(formData.longitude) || 79.8612;
  const delta = 0.008;
  const bbox = `${lon - delta},${lat - delta},${lon + delta},${lat + delta}`;
  const osmEmbedUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat},${lon}`;

  // Live password complexity computations
  const hasMinLength = formData.password.length >= 6;
  const hasUppercase = /[A-Z]/.test(formData.password);
  const hasLowercase = /[a-z]/.test(formData.password);
  const hasNumber = /[0-9]/.test(formData.password);
  const hasSymbol = /[^A-Za-z0-9]/.test(formData.password);
  const isPasswordComplex = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSymbol;

  const isPasswordShort = formData.password.length > 0 && formData.password.length < 6;
  const isPasswordMismatch = formData.confirmPassword.length > 0 && formData.password !== formData.confirmPassword;
  const isPasswordMatch = formData.confirmPassword.length > 0 && formData.password === formData.confirmPassword && isPasswordComplex;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Basic validation
    if (!formData.nic.trim() || !formData.username.trim() || !formData.fullName.trim() || !formData.email.trim() || !formData.phone.trim() || !formData.address.trim() || !formData.password.trim() || !formData.confirmPassword.trim()) {
      setError('Please fill in all mandatory fields, including the physical address.');
      return;
    }

    if (!isPasswordComplex) {
      setError('Password must be at least 6 characters and include uppercase, lowercase, a number, and a symbol.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Password and Confirm Password do not match.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        nic: formData.nic.trim(),
        username: formData.username.trim(),
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        address: formData.address.trim(),
        latitude: parseFloat(formData.latitude) || null,
        longitude: parseFloat(formData.longitude) || null,
        password: formData.password,
        role: formData.role,
      };

      await adminService.createStaffUser(payload);
      onStaffCreated();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to create staff user account.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: '640px', maxHeight: '92vh', overflowY: 'auto' }} onClick={(e) => e.stopPropagation()}>
        <div className="card-header" style={{ position: 'sticky', top: 0, zIndex: 10, background: 'var(--bg-surface)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              background: 'var(--role-backoffice-bg)',
              color: 'var(--role-backoffice)',
              padding: '0.5rem',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <UserPlus size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', margin: 0 }}>Create Staff Account</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>Provision Backoffice or Grid Operator credentials</p>
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

            {/* Automated Email Notice Banner */}
            <div style={{
              background: 'rgba(245, 158, 11, 0.08)',
              border: '1px solid rgba(245, 158, 11, 0.25)',
              borderRadius: 'var(--radius-md)',
              padding: '0.75rem 1rem',
              marginBottom: '1.25rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.65rem',
              fontSize: '0.8rem',
              color: 'var(--solar-amber)'
            }}>
              <Mail size={18} style={{ flexShrink: 0 }} />
              <span>An automated onboarding email with login credentials (username & temporary password) will be delivered to the specified email address upon creation.</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label">National Identity Card (NIC) *</label>
                <input
                  type="text"
                  name="nic"
                  className="form-control"
                  placeholder="e.g. 199012345678"
                  value={formData.nic}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Username *</label>
                <input
                  type="text"
                  name="username"
                  className="form-control"
                  placeholder="e.g. officer_perera"
                  value={formData.username}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Full Legal Name *</label>
              <input
                type="text"
                name="fullName"
                className="form-control"
                placeholder="e.g. Sunil Perera"
                value={formData.fullName}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Email Address (For Credentials Dispatch) *</label>
              <input
                type="email"
                name="email"
                className="form-control"
                placeholder="e.g. officer.perera@smartgrid.lk"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Phone Number *</label>
                <input
                  type="tel"
                  name="phone"
                  className="form-control"
                  placeholder="e.g. 0771234567"
                  value={formData.phone}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Role Assignment *</label>
                <select
                  name="role"
                  className="form-control"
                  value={formData.role}
                  onChange={handleChange}
                >
                  <option value="Backoffice">Backoffice Officer</option>
                  <option value="GridOperator">Grid Operator</option>
                </select>
              </div>
            </div>

            {/* Address Field */}
            <div className="form-group">
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <MapPin size={15} style={{ color: 'var(--solar-amber)' }} />
                <span>Physical Address / Operational Station *</span>
              </label>
              <input
                type="text"
                name="address"
                className="form-control"
                placeholder="e.g. No. 50, Microgrid Operations Center, Colombo 02"
                value={formData.address}
                onChange={handleChange}
                required
              />
            </div>

            {/* Interactive Location Map Picker Section */}
            <div style={{
              background: 'rgba(15, 23, 42, 0.65)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '1rem',
              marginTop: '0.5rem',
              marginBottom: '1rem'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                  <Compass size={16} style={{ color: 'var(--solar-amber)' }} />
                  <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-main)' }}>
                    Station / Facility Location Coordinates (Interactive Map)
                  </span>
                </div>
                <div style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontFamily: 'monospace',
                  fontSize: '0.75rem',
                  color: 'var(--solar-amber)',
                  background: 'rgba(245, 158, 11, 0.12)',
                  padding: '0.2rem 0.55rem',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid rgba(245, 158, 11, 0.25)'
                }}>
                  <MapPin size={12} />
                  <span>{lat.toFixed(5)}° N, {lon.toFixed(5)}° E</span>
                </div>
              </div>

              {/* Quick Substation Presets */}
              <div style={{ marginBottom: '0.75rem' }}>
                <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginBottom: '0.35rem', textTransform: 'uppercase', fontWeight: 600 }}>
                  Quick Select Microgrid Substation:
                </p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                  {STATION_PRESETS.map((preset) => (
                    <button
                      key={preset.name}
                      type="button"
                      onClick={() => handleSelectPreset(preset)}
                      style={{
                        fontSize: '0.72rem',
                        padding: '0.25rem 0.55rem',
                        borderRadius: 'var(--radius-sm)',
                        border: (Math.abs(lat - preset.lat) < 0.001 && Math.abs(lon - preset.lon) < 0.001)
                          ? '1px solid var(--solar-amber)'
                          : '1px solid var(--border-light)',
                        background: (Math.abs(lat - preset.lat) < 0.001 && Math.abs(lon - preset.lon) < 0.001)
                          ? 'rgba(245, 158, 11, 0.2)'
                          : 'rgba(255, 255, 255, 0.04)',
                        color: (Math.abs(lat - preset.lat) < 0.001 && Math.abs(lon - preset.lon) < 0.001)
                          ? 'var(--solar-amber)'
                          : 'var(--text-muted)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      {preset.name}
                    </button>
                  ))}
                </div>
              </div>

              {/* Map Iframe Live Preview */}
              <div style={{
                position: 'relative',
                width: '100%',
                height: '180px',
                borderRadius: 'var(--radius-sm)',
                overflow: 'hidden',
                border: '1px solid var(--border-subtle)',
                marginBottom: '0.75rem',
                background: '#1a233a'
              }}>
                <iframe
                  key={`${lat}-${lon}`}
                  title="Staff Station Map"
                  width="100%"
                  height="100%"
                  frameBorder="0"
                  scrolling="no"
                  src={osmEmbedUrl}
                  style={{ border: 0 }}
                />
              </div>

              {/* Manual Coordinates Input Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: '0.72rem' }}>Latitude (°N)</label>
                  <input
                    type="number"
                    step="0.000001"
                    name="latitude"
                    className="form-control"
                    value={formData.latitude}
                    onChange={handleChange}
                    placeholder="6.9271"
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontSize: '0.72rem' }}>Longitude (°E)</label>
                  <input
                    type="number"
                    step="0.000001"
                    name="longitude"
                    className="form-control"
                    value={formData.longitude}
                    onChange={handleChange}
                    placeholder="79.8612"
                  />
                </div>
              </div>
            </div>

            {/* Password and Confirm Password with Live Validation */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Initial Password *</label>
                <input
                  type="password"
                  name="password"
                  className="form-control"
                  placeholder="e.g. Solar#2026"
                  value={formData.password}
                  onChange={handleChange}
                  style={{
                    borderColor: isPasswordShort
                      ? 'var(--status-deactivated)'
                      : isPasswordComplex
                      ? 'rgba(16, 185, 129, 0.5)'
                      : undefined
                  }}
                  required
                />
                {isPasswordShort && (
                  <span style={{ color: 'var(--status-deactivated)', fontSize: '0.725rem', marginTop: '0.25rem', display: 'block' }}>
                    Password must be at least 6 characters
                  </span>
                )}
                {isPasswordComplex && (
                  <span style={{ color: 'var(--status-active)', fontSize: '0.725rem', marginTop: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <Check size={12} /> Strong password
                  </span>
                )}
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Confirm Password *</label>
                <input
                  type="password"
                  name="confirmPassword"
                  className="form-control"
                  placeholder="Re-enter password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  style={{
                    borderColor: isPasswordMismatch
                      ? 'var(--status-deactivated)'
                      : isPasswordMatch
                      ? 'rgba(16, 185, 129, 0.5)'
                      : undefined
                  }}
                  required
                />
                {isPasswordMismatch && (
                  <span style={{ color: 'var(--status-deactivated)', fontSize: '0.725rem', marginTop: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <AlertCircle size={12} /> Passwords do not match
                  </span>
                )}
                {isPasswordMatch && (
                  <span style={{ color: 'var(--status-active)', fontSize: '0.725rem', marginTop: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <CheckCircle2 size={12} /> Passwords match
                  </span>
                )}
              </div>
            </div>

            {/* Live Password Complexity Checklist */}
            {formData.password.length > 0 && (
              <div style={{
                marginTop: '0.75rem',
                padding: '0.6rem 0.75rem',
                background: 'rgba(255, 255, 255, 0.03)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.35rem'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '0.72rem', fontWeight: 600, color: isPasswordComplex ? 'var(--status-active)' : 'var(--text-muted)' }}>
                    Password Requirements:
                  </span>
                  <span style={{ fontSize: '0.7rem', color: isPasswordComplex ? 'var(--status-active)' : 'var(--solar-amber)' }}>
                    {isPasswordComplex ? 'All requirements met' : 'Follow rules below'}
                  </span>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.2rem',
                    padding: '0.15rem 0.45rem',
                    borderRadius: '999px',
                    fontSize: '0.68rem',
                    background: hasMinLength ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                    color: hasMinLength ? 'var(--status-active)' : 'var(--status-deactivated)',
                    border: `1px solid ${hasMinLength ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                  }}>
                    {hasMinLength ? <Check size={10} /> : <AlertCircle size={10} />} 6+ Chars
                  </span>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.2rem',
                    padding: '0.15rem 0.45rem',
                    borderRadius: '999px',
                    fontSize: '0.68rem',
                    background: hasUppercase ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                    color: hasUppercase ? 'var(--status-active)' : 'var(--status-deactivated)',
                    border: `1px solid ${hasUppercase ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                  }}>
                    {hasUppercase ? <Check size={10} /> : <AlertCircle size={10} />} Uppercase (A-Z)
                  </span>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.2rem',
                    padding: '0.15rem 0.45rem',
                    borderRadius: '999px',
                    fontSize: '0.68rem',
                    background: hasLowercase ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                    color: hasLowercase ? 'var(--status-active)' : 'var(--status-deactivated)',
                    border: `1px solid ${hasLowercase ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                  }}>
                    {hasLowercase ? <Check size={10} /> : <AlertCircle size={10} />} Lowercase (a-z)
                  </span>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.2rem',
                    padding: '0.15rem 0.45rem',
                    borderRadius: '999px',
                    fontSize: '0.68rem',
                    background: hasNumber ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                    color: hasNumber ? 'var(--status-active)' : 'var(--status-deactivated)',
                    border: `1px solid ${hasNumber ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                  }}>
                    {hasNumber ? <Check size={10} /> : <AlertCircle size={10} />} Number (0-9)
                  </span>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.2rem',
                    padding: '0.15rem 0.45rem',
                    borderRadius: '999px',
                    fontSize: '0.68rem',
                    background: hasSymbol ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                    color: hasSymbol ? 'var(--status-active)' : 'var(--status-deactivated)',
                    border: `1px solid ${hasSymbol ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                  }}>
                    {hasSymbol ? <Check size={10} /> : <AlertCircle size={10} />} Symbol (@#$%)
                  </span>
                </div>
              </div>
            )}
          </div>

          <div style={{
            padding: '1rem 1.5rem',
            background: 'var(--bg-card-header)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem',
            position: 'sticky',
            bottom: 0,
            zIndex: 10
          }}>
            <button type="button" className="btn btn-outline" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || !isPasswordComplex || isPasswordMismatch || !formData.confirmPassword}
            >
              {loading ? <span className="spinner"></span> : <Check size={18} />}
              <span>{loading ? 'Creating & Dispatching Email...' : 'Create Account'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
