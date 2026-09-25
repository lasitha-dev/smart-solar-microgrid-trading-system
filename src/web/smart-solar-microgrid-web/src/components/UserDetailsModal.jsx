/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: User details inspector modal showing complete account attributes, facility coordinates, and View on Map action.
 */

import React, { useState } from 'react';
import { X, User, MapPin, Phone, Mail, Calendar, Shield, Activity, Map } from 'lucide-react';
import { LocationMapModal } from './LocationMapModal';

export const UserDetailsModal = ({ isOpen, onClose, user, onViewMap }) => {
  const [internalMapOpen, setInternalMapOpen] = useState(false);

  if (!isOpen || !user) return null;

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'Active':
        return <span className="badge badge-active">Active</span>;
      case 'PendingActivation':
        return <span className="badge badge-pending">Pending Approval</span>;
      case 'Deactivated':
        return <span className="badge badge-deactivated">Deactivated</span>;
      default:
        return <span className="badge">{status}</span>;
    }
  };

  const getRoleBadge = (role) => {
    switch (role) {
      case 'Backoffice':
      case 'Administrator':
        return <span className="badge badge-role-backoffice">{role}</span>;
      case 'GridOperator':
        return <span className="badge badge-role-operator">Grid Operator</span>;
      case 'Prosumer':
        return <span className="badge badge-role-prosumer">Solar Prosumer</span>;
      default:
        return <span className="badge">{role}</span>;
    }
  };

  const handleOpenMap = () => {
    if (onViewMap) {
      onViewMap(user);
    } else {
      setInternalMapOpen(true);
    }
  };

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-content" style={{ maxWidth: '600px' }} onClick={(e) => e.stopPropagation()}>
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{
                background: 'var(--solar-amber-light)',
                color: 'var(--solar-amber)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <User size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: '1.15rem' }}>{user.fullName || user.username}</h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Account ID: {user.id || user.userId || 'N/A'}</p>
              </div>
            </div>
            <button className="btn btn-outline btn-sm" onClick={onClose} style={{ border: 'none', padding: '0.35rem' }}>
              <X size={20} />
            </button>
          </div>

          <div className="card-body">
            <div className="responsive-grid-2" style={{ gap: '1.25rem', marginBottom: '1.5rem' }}>
              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Account Status
                </p>
                <div>{getStatusBadge(user.status)}</div>
              </div>

              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  System Role
                </p>
                <div>{getRoleBadge(user.role)}</div>
              </div>

              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  National Identity Card (NIC)
                </p>
                <p style={{ fontWeight: 600 }}>{user.nic}</p>
              </div>

              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Username
                </p>
                <p style={{ fontWeight: 600 }}>{user.username}</p>
              </div>

              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Email Address
                </p>
                <p style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', wordBreak: 'break-all' }}>
                  <Mail size={14} style={{ color: 'var(--solar-amber)' }} />
                  <span>{user.email || 'Not provided'}</span>
                </p>
              </div>

              <div>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Contact Phone
                </p>
                <p style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Phone size={14} style={{ color: 'var(--solar-amber)' }} />
                  <span>{user.phone || 'Not provided'}</span>
                </p>
              </div>

              <div style={{ gridColumn: 'span 2' }}>
                <p style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Registration Date
                </p>
                <p style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
                  <Calendar size={14} style={{ color: 'var(--text-muted)' }} />
                  <span>{formatDate(user.createdAt)}</span>
                </p>
              </div>
            </div>

            {/* Facility Location Section for Prosumers */}
            <div style={{
              background: 'var(--bg-main)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '1rem',
              marginTop: '0.5rem'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <MapPin size={16} style={{ color: 'var(--solar-amber)' }} />
                  <h4 style={{ fontSize: '0.9rem', color: 'var(--text-main)' }}>Solar Facility & Node Location</h4>
                </div>
              </div>

              <div style={{ marginBottom: '0.65rem' }}>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Street Address:</p>
                <p style={{ fontSize: '0.9rem', fontWeight: 500 }}>{user.address || 'Address not registered'}</p>
              </div>

              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '0.75rem',
                paddingTop: '0.5rem',
                borderTop: '1px solid var(--border-subtle)'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                  <div>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Latitude:</p>
                    <p style={{ fontSize: '0.85rem', fontWeight: 600, fontFamily: 'monospace', color: 'var(--solar-amber)' }}>
                      {user.latitude !== undefined && user.latitude !== null ? `${user.latitude.toFixed(5)}° N` : 'N/A'}
                    </p>
                  </div>
                  <div>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Longitude:</p>
                    <p style={{ fontSize: '0.85rem', fontWeight: 600, fontFamily: 'monospace', color: 'var(--solar-amber)' }}>
                      {user.longitude !== undefined && user.longitude !== null ? `${user.longitude.toFixed(5)}° E` : 'N/A'}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    fontSize: '0.75rem',
                    padding: '0.3rem 0.75rem'
                  }}
                  onClick={handleOpenMap}
                  title="View coordinates on interactive map"
                >
                  <MapPin size={13} />
                  <span>View on Map</span>
                </button>
              </div>
            </div>
          </div>

          <div style={{
            padding: '1rem 1.5rem',
            background: 'var(--bg-card-header)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              style={{
                color: 'var(--solar-amber)',
                borderColor: 'rgba(245, 158, 11, 0.4)',
                background: 'rgba(245, 158, 11, 0.08)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem'
              }}
              onClick={handleOpenMap}
            >
              <MapPin size={14} />
              <span>Open Location Map</span>
            </button>

            <button type="button" className="btn btn-outline btn-sm" onClick={onClose}>
              Close Inspector
            </button>
          </div>
        </div>
      </div>

      {/* Internal Map Modal if triggered from inside inspector */}
      <LocationMapModal
        isOpen={internalMapOpen}
        onClose={() => setInternalMapOpen(false)}
        user={user}
      />
    </>
  );
};
