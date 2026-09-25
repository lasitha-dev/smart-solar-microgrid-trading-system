/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Premium Modal Component displaying full details, timeline, and QR token for a reservation.
 */

import React, { useState } from 'react';
import StatusBadge from './components/StatusBadge';

const ReservationDetailModal = ({ reservation, onClose, onApprove, onCancel }) => {
  const [copied, setCopied] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [showCancelInput, setShowCancelInput] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  if (!reservation) return null;

  const handleCopyQr = () => {
    if (reservation.qrCode) {
      navigator.clipboard.writeText(reservation.qrCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleConfirmCancel = async () => {
    setActionLoading(true);
    try {
      await onCancel(reservation.id, cancelReason || 'Operator cancelled');
      onClose();
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmApprove = async () => {
    setActionLoading(true);
    try {
      await onApprove(reservation.id);
      onClose();
    } finally {
      setActionLoading(false);
    }
  };

  const steps = [
    { label: 'Requested', done: true },
    { label: 'Approved', done: reservation.status === 'Approved' || reservation.status === 'Completed' },
    { label: 'Scheduled', done: reservation.status !== 'Cancelled' },
    { label: 'Completed', done: reservation.status === 'Completed' }
  ];

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(6px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1050,
        padding: '16px',
        animation: 'fadeIn 0.2s ease-out'
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: '#FFFFFF',
          borderRadius: '16px',
          width: '100%',
          maxWidth: '620px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: '90vh'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            background: 'linear-gradient(135deg, #1B5E20 0%, #2E7D32 100%)',
            padding: '20px 24px',
            color: '#FFFFFF',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '1.6rem' }}>⚡</span>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700 }}>Reservation Receipt</h3>
              <p style={{ margin: 0, fontSize: '0.85rem', opacity: 0.85 }}>Smart Solar Microgrid Trading System</p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255, 255, 255, 0.2)',
              border: 'none',
              borderRadius: '50%',
              width: '32px',
              height: '32px',
              color: '#FFFFFF',
              fontSize: '1.1rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
          {/* Status & ID Badge Header */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              paddingBottom: '16px',
              borderBottom: '1px solid #E2E8F0',
              marginBottom: '20px'
            }}
          >
            <div>
              <span style={{ fontSize: '0.8rem', color: '#64748B', fontWeight: 600, textTransform: 'uppercase' }}>
                Booking Reference
              </span>
              <div style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '1.05rem', color: '#0F172A' }}>
                {reservation.id}
              </div>
            </div>
            <StatusBadge status={reservation.status} />
          </div>

          {/* Stepper Timeline */}
          {reservation.status !== 'Cancelled' ? (
            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '0.8rem', color: '#64748B', fontWeight: 600, textTransform: 'uppercase' }}>
                Workflow Progress
              </span>
              <div style={{ display: 'flex', alignItems: 'center', marginTop: '12px', position: 'relative' }}>
                {steps.map((step, idx) => (
                  <React.Fragment key={step.label}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', zIndex: 2, flex: 1 }}>
                      <div
                        style={{
                          width: '28px',
                          height: '28px',
                          borderRadius: '50%',
                          backgroundColor: step.done ? '#2E7D32' : '#CBD5E1',
                          color: '#FFFFFF',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '0.8rem',
                          fontWeight: 700,
                          transition: 'all 0.3s'
                        }}
                      >
                        {step.done ? '✓' : idx + 1}
                      </div>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          marginTop: '6px',
                          fontWeight: step.done ? 600 : 400,
                          color: step.done ? '#1E293B' : '#94A3B8'
                        }}
                      >
                        {step.label}
                      </span>
                    </div>
                    {idx < steps.length - 1 && (
                      <div
                        style={{
                          position: 'absolute',
                          top: '14px',
                          left: `${(idx / (steps.length - 1)) * 100 + 12}%`,
                          width: `${100 / (steps.length - 1) - 24}%`,
                          height: '2px',
                          backgroundColor: steps[idx + 1].done ? '#2E7D32' : '#E2E8F0',
                          zIndex: 1
                        }}
                      />
                    )}
                  </React.Fragment>
                ))}
              </div>
            </div>
          ) : (
            <div
              style={{
                backgroundColor: '#FEF2F2',
                border: '1px solid #FECACA',
                borderRadius: '8px',
                padding: '12px 16px',
                marginBottom: '20px',
                color: '#991B1B'
              }}
            >
              <strong>Booking Cancelled:</strong> {reservation.cancelReason || 'No reason provided.'}
            </div>
          )}

          {/* Details Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, 1fr)',
              gap: '16px',
              backgroundColor: '#F8FAFC',
              padding: '16px',
              borderRadius: '12px',
              border: '1px solid #E2E8F0'
            }}
          >
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600 }}>PROSUMER NIC</span>
              <p style={{ margin: '4px 0 0', fontWeight: 600, color: '#1E293B' }}>{reservation.prosumerId}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600 }}>STATION ID</span>
              <p style={{ margin: '4px 0 0', fontWeight: 600, color: '#1E293B' }}>{reservation.stationId}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600 }}>SLOT IDENTIFIER</span>
              <p style={{ margin: '4px 0 0', fontWeight: 600, color: '#1E293B' }}>{reservation.bookingSlotId}</p>
            </div>
            <div>
              <span style={{ fontSize: '0.75rem', color: '#64748B', fontWeight: 600 }}>SCHEDULED DATE & TIME</span>
              <p style={{ margin: '4px 0 0', fontWeight: 600, color: '#1E293B' }}>
                {new Date(reservation.scheduledDateTime).toLocaleString(undefined, {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit'
                })}
              </p>
            </div>
          </div>

          {/* QR Code Payload */}
          {reservation.qrCode && (
            <div
              style={{
                marginTop: '20px',
                padding: '16px',
                backgroundColor: '#F0FDF4',
                border: '1px solid #BBF7D0',
                borderRadius: '12px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#166534' }}>
                  QR Verification Token
                </span>
                <button
                  onClick={handleCopyQr}
                  style={{
                    background: '#FFFFFF',
                    border: '1px solid #86EFAC',
                    borderRadius: '6px',
                    padding: '4px 10px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    color: '#166534',
                    cursor: 'pointer'
                  }}
                >
                  {copied ? '✓ Copied!' : '📋 Copy Token'}
                </button>
              </div>
              <div
                style={{
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  backgroundColor: '#FFFFFF',
                  padding: '10px',
                  borderRadius: '6px',
                  marginTop: '8px',
                  wordBreak: 'break-all',
                  color: '#14532D',
                  border: '1px dashed #86EFAC'
                }}
              >
                {reservation.qrCode}
              </div>
            </div>
          )}

          {/* Cancel Reason Prompt */}
          {showCancelInput && (
            <div style={{ marginTop: '16px', padding: '16px', backgroundColor: '#FFF1F2', borderRadius: '10px' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: '#9F1239' }}>
                Provide Cancellation Reason:
              </label>
              <input
                type="text"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="e.g. Station offline for solar panel maintenance"
                style={{
                  width: '100%',
                  marginTop: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  border: '1px solid #FECDD3',
                  fontSize: '0.9rem'
                }}
              />
              <div style={{ display: 'flex', gap: '8px', marginTop: '10px', justifyContent: 'flex-end' }}>
                <button
                  onClick={() => setShowCancelInput(false)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '6px',
                    background: 'transparent',
                    border: '1px solid #CBD5E1',
                    cursor: 'pointer'
                  }}
                >
                  Back
                </button>
                <button
                  onClick={handleConfirmCancel}
                  disabled={actionLoading}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '6px',
                    background: '#E11D48',
                    color: '#FFF',
                    border: 'none',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  {actionLoading ? 'Cancelling...' : 'Confirm Cancellation'}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div
          style={{
            padding: '16px 24px',
            background: '#F8FAFC',
            borderTop: '1px solid #E2E8F0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <div>
            {reservation.status !== 'Cancelled' && !showCancelInput && onCancel && (
              <button
                onClick={() => setShowCancelInput(true)}
                style={{
                  background: '#FFF',
                  color: '#DC2626',
                  border: '1px solid #FCA5A5',
                  borderRadius: '8px',
                  padding: '8px 16px',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  cursor: 'pointer'
                }}
              >
                Cancel Booking
              </button>
            )}
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              onClick={onClose}
              style={{
                background: '#FFF',
                color: '#475569',
                border: '1px solid #CBD5E1',
                borderRadius: '8px',
                padding: '8px 18px',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer'
              }}
            >
              Close
            </button>

            {reservation.status === 'Pending' && onApprove && (
              <button
                onClick={handleConfirmApprove}
                disabled={actionLoading}
                style={{
                  background: '#16A34A',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '8px 20px',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  boxShadow: '0 2px 4px rgba(22, 163, 74, 0.2)'
                }}
              >
                {actionLoading ? 'Approving...' : '✓ Approve Reservation'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReservationDetailModal;
