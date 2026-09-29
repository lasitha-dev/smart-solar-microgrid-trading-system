/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Confirmation modal dialog for station node deactivation with FAT service business rule error enforcement.
 * Author: Member 2
 */

import React from 'react';
import {
  X,
  AlertTriangle,
  XCircle,
  AlertCircle,
  ShieldAlert
} from 'lucide-react';

export const StationDeactivateModal = ({
  isOpen,
  onClose,
  onConfirm,
  station,
  isLoading = false,
  error = ''
}) => {
  if (!isOpen || !station) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        style={{ maxWidth: '520px' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                background: 'var(--status-deactivated-bg)',
                color: 'var(--status-deactivated)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <AlertTriangle size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem' }}>Deactivate Station Node</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Target: <strong style={{ color: 'var(--text-main)' }}>{station.stationName}</strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-outline btn-sm"
            onClick={onClose}
            style={{ border: 'none', padding: '0.35rem' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Prominent Red Alert box if server rejects deactivation (e.g. 409 Conflict with Active Reservations) */}
          {error && (
            <div
              className="alert alert-danger"
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '0.5rem',
                padding: '0.85rem 1rem',
                borderLeft: '4px solid #ef4444'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600 }}>
                <ShieldAlert size={18} style={{ color: '#ef4444', flexShrink: 0 }} />
                <span>Deactivation Prohibited (FAT Rule Violation)</span>
              </div>
              <p style={{ fontSize: '0.85rem', margin: 0, lineHeight: 1.4 }}>
                {error}
              </p>
              <p style={{ fontSize: '0.75rem', margin: 0, color: 'var(--text-muted)' }}>
                All pending or approved trading reservations must be completed or cancelled before this station can be decommissioned.
              </p>
            </div>
          )}

          <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', lineHeight: 1.5, margin: 0 }}>
            Are you sure you want to deactivate <strong style={{ color: 'var(--text-main)' }}>{station.stationName}</strong>?
          </p>

          <div
            style={{
              background: 'var(--bg-card)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-sm)',
              padding: '0.75rem 1rem',
              fontSize: '0.825rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.35rem'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Node Identifier:</span>
              <span style={{ fontFamily: 'monospace' }}>{station.id}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Storage Capacity:</span>
              <span>{station.capacityKwh} kWh</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Total Battery Bays:</span>
              <span>{station.totalBatterySlots ?? station.batterySlots?.length ?? 0} Bays</span>
            </div>
          </div>

          <p style={{ fontSize: '0.8rem', color: 'var(--text-dim)', margin: 0 }}>
            Once deactivated, prosumers will no longer be able to select this station for battery charging or energy slot reservations.
          </p>
        </div>

        {/* Modal Footer */}
        <div
          style={{
            padding: '1rem 1.5rem',
            background: 'var(--bg-card-header)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem'
          }}
        >
          <button
            type="button"
            className="btn btn-outline"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={() => onConfirm(station.id)}
            disabled={isLoading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            {isLoading ? <span className="spinner" /> : <XCircle size={16} />}
            <span>{isLoading ? 'Deactivating...' : 'Confirm Deactivation'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
