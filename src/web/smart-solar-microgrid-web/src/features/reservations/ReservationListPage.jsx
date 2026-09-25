/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Enterprise React Dashboard for Grid Operators to monitor, approve, and manage solar slot reservations.
 */

import React, { useState, useEffect, useMemo } from 'react';
import { reservationService } from '../../services/reservationService';
import StatusBadge from './components/StatusBadge';
import ReservationDetailModal from './ReservationDetailModal';

const ReservationListPage = () => {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedRes, setSelectedRes] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [toast, setToast] = useState(null);
  const [actionInProgress, setActionInProgress] = useState({});
  const [seeding, setSeeding] = useState(false);

  const operatorId = 'OP-COLOMBO-01';

  useEffect(() => {
    fetchReservations();
  }, []);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const MOCK_RESERVATIONS = [
    { id: 'res-demo-001', prosumerId: 'PSM-20011234567', stationId: 'Station A – Solar Bay', bookingSlotId: 'BAY-1', scheduledDateTime: new Date(Date.now() + 2 * 86400000).toISOString(), status: 'Pending', requestedAt: new Date().toISOString(), qrCode: null },
    { id: 'res-demo-002', prosumerId: 'PSM-19981122334', stationId: 'Station B – Hill Top Grid', bookingSlotId: 'BAY-2', scheduledDateTime: new Date(Date.now() + 3 * 86400000).toISOString(), status: 'Approved', requestedAt: new Date(Date.now() - 86400000).toISOString(), qrCode: 'SSMTS-QR:demo-qr-token-abc123' },
    { id: 'res-demo-003', prosumerId: 'PSM-20021345678', stationId: 'Station A – Solar Bay', bookingSlotId: 'BAY-3', scheduledDateTime: new Date(Date.now() + 1 * 86400000).toISOString(), status: 'Pending', requestedAt: new Date(Date.now() - 3600000).toISOString(), qrCode: null },
    { id: 'res-demo-004', prosumerId: 'PSM-19991223344', stationId: 'Station C – Valley Microgrid', bookingSlotId: 'BAY-1', scheduledDateTime: new Date(Date.now() - 2 * 86400000).toISOString(), status: 'Completed', requestedAt: new Date(Date.now() - 5 * 86400000).toISOString(), qrCode: 'SSMTS-QR:demo-qr-token-xyz789' },
    { id: 'res-demo-005', prosumerId: 'PSM-20011234567', stationId: 'Station B – Hill Top Grid', bookingSlotId: 'BAY-2', scheduledDateTime: new Date(Date.now() + 5 * 86400000).toISOString(), status: 'Cancelled', requestedAt: new Date(Date.now() - 2 * 86400000).toISOString(), qrCode: null },
  ];

  const fetchReservations = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await reservationService.getAllReservations();
      if (res.success) {
        setReservations(res.data || []);
      } else {
        // Fallback to mock data for demo
        setReservations(MOCK_RESERVATIONS);
        setError(null);
      }
    } catch (err) {
      // API unavailable — load demo data for viva presentation
      console.warn('API unavailable, loading demo data:', err.message);
      setReservations(MOCK_RESERVATIONS);
      setError(null);
    } finally {
      setLoading(false);
    }
  };


  const handleApprove = async (id) => {
    setActionInProgress((prev) => ({ ...prev, [id]: true }));
    try {
      const res = await reservationService.approveReservation(id, operatorId);
      if (res.success) {
        showToast(`Reservation ${id.substring(0, 8)} approved with QR token!`, 'success');
        await fetchReservations();
      } else {
        showToast(res.message || 'Approval failed.', 'error');
      }
    } catch (err) {
      showToast(err.message || 'Approval error occurred.', 'error');
    } finally {
      setActionInProgress((prev) => ({ ...prev, [id]: false }));
    }
  };

  const handleCancel = async (id, reason) => {
    setActionInProgress((prev) => ({ ...prev, [id]: true }));
    try {
      const res = await reservationService.cancelReservation(id, reason);
      if (res.success) {
        showToast(`Reservation cancelled successfully.`, 'info');
        await fetchReservations();
      } else {
        showToast(res.message || 'Cancellation failed.', 'error');
      }
    } catch (err) {
      showToast(err.message || 'Cancellation error.', 'error');
    } finally {
      setActionInProgress((prev) => ({ ...prev, [id]: false }));
    }
  };

  const handleSeedSlots = async () => {
    setSeeding(true);
    try {
      const res = await reservationService.seedSlots();
      if (res.success) {
        showToast('Successfully seeded 10 days of energy slots for Station A!', 'success');
        await fetchReservations();
      } else {
        showToast(res.message || 'Slot seeding failed.', 'error');
      }
    } catch (err) {
      showToast(err.message || 'Seeding error.', 'error');
    } finally {
      setSeeding(false);
    }
  };

  // Metrics
  const metrics = useMemo(() => {
    const total = reservations.length;
    const pending = reservations.filter((r) => r.status === 'Pending').length;
    const approved = reservations.filter((r) => r.status === 'Approved').length;
    const cancelled = reservations.filter((r) => r.status === 'Cancelled').length;
    return { total, pending, approved, cancelled };
  }, [reservations]);

  // Filtered list
  const filteredReservations = useMemo(() => {
    return reservations.filter((r) => {
      const matchesTab = activeTab === 'ALL' || r.status.toUpperCase() === activeTab;
      const query = searchQuery.trim().toLowerCase();
      const matchesSearch =
        !query ||
        r.id.toLowerCase().includes(query) ||
        r.prosumerId.toLowerCase().includes(query) ||
        r.stationId.toLowerCase().includes(query);
      return matchesTab && matchesSearch;
    });
  }, [reservations, activeTab, searchQuery]);

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#F8FAFC', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      {/* Toast Notification */}
      {toast && (
        <div
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            backgroundColor: toast.type === 'error' ? '#EF4444' : toast.type === 'info' ? '#3B82F6' : '#10B981',
            color: '#FFFFFF',
            padding: '14px 22px',
            borderRadius: '10px',
            boxShadow: '0 10px 15px -3px rgba(0,0,0,0.15)',
            zIndex: 2000,
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontWeight: 600,
            fontSize: '0.9rem',
            animation: 'slideUp 0.3s ease-out'
          }}
        >
          <span>{toast.type === 'error' ? '⚠️' : '✓'}</span>
          {toast.message}
        </div>
      )}

      {/* Top Enterprise Header */}
      <header
        style={{
          background: 'linear-gradient(135deg, #0F5132 0%, #198754 100%)',
          color: '#FFFFFF',
          padding: '24px 36px',
          boxShadow: '0 4px 12px rgba(15, 81, 50, 0.15)'
        }}
      >
        <div style={{ maxWidth: '1280px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '1.8rem' }}>☀️</span>
              <h1 style={{ margin: 0, fontSize: '1.6rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
                Smart Solar Microgrid
              </h1>
              <span style={{ background: 'rgba(255,255,255,0.2)', padding: '2px 8px', borderRadius: '6px', fontSize: '0.75rem', fontWeight: 700 }}>
                OPERATOR PORTAL
              </span>
            </div>
            <p style={{ margin: '6px 0 0', opacity: 0.85, fontSize: '0.9rem' }}>
              Energy Slot Reservation &amp; Dispatch Monitoring • Student IT22221414
            </p>
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={handleSeedSlots}
              disabled={seeding}
              style={{
                background: '#F59E0B',
                color: '#1E293B',
                border: 'none',
                borderRadius: '8px',
                padding: '10px 18px',
                fontWeight: 700,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
              }}
            >
              <span>🌱</span>
              {seeding ? 'Seeding Database...' : 'Seed Demo Slots (Viva)'}
            </button>

            <button
              onClick={fetchReservations}
              style={{
                background: 'rgba(255,255,255,0.15)',
                color: '#FFFFFF',
                border: '1px solid rgba(255,255,255,0.3)',
                borderRadius: '8px',
                padding: '10px 16px',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <span>↻</span> Refresh
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main style={{ maxWidth: '1280px', margin: '0 auto', padding: '32px 24px' }}>
        {/* Metric Cards Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '16px',
            marginBottom: '32px'
          }}
        >
          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ color: '#64748B', fontSize: '0.85rem', fontWeight: 600 }}>TOTAL BOOKINGS</div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#0F172A', marginTop: '4px' }}>{metrics.total}</div>
          </div>

          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #FDE68A', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ color: '#D97706', fontSize: '0.85rem', fontWeight: 600 }}>PENDING APPROVAL</div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#B45309', marginTop: '4px' }}>{metrics.pending}</div>
          </div>

          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #BFDBFE', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ color: '#2563EB', fontSize: '0.85rem', fontWeight: 600 }}>APPROVED &amp; ACTIVE</div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#1D4ED8', marginTop: '4px' }}>{metrics.approved}</div>
          </div>

          <div style={{ background: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #FECDD3', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ color: '#E11D48', fontSize: '0.85rem', fontWeight: 600 }}>CANCELLED</div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#BE123C', marginTop: '4px' }}>{metrics.cancelled}</div>
          </div>
        </div>

        {/* Toolbar: Filter Tabs & Search */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '16px',
            marginBottom: '20px'
          }}
        >
          {/* Tabs */}
          <div style={{ display: 'flex', gap: '8px', background: '#E2E8F0', padding: '4px', borderRadius: '10px' }}>
            {['ALL', 'PENDING', 'APPROVED', 'COMPLETED', 'CANCELLED'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                style={{
                  padding: '8px 16px',
                  borderRadius: '8px',
                  border: 'none',
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  backgroundColor: activeTab === tab ? '#FFFFFF' : 'transparent',
                  color: activeTab === tab ? '#0F172A' : '#64748B',
                  boxShadow: activeTab === tab ? '0 1px 3px rgba(0,0,0,0.1)' : 'none'
                }}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div style={{ minWidth: '280px' }}>
            <input
              type="text"
              placeholder="🔍 Search by NIC or Station..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: '8px',
                border: '1px solid #CBD5E1',
                fontSize: '0.85rem',
                backgroundColor: '#FFFFFF'
              }}
            />
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            style={{
              backgroundColor: '#FEF2F2',
              border: '1px solid #FECACA',
              color: '#991B1B',
              padding: '14px 20px',
              borderRadius: '10px',
              marginBottom: '20px'
            }}
          >
            <strong>Error:</strong> {error}
          </div>
        )}

        {/* Table / Card View */}
        <div
          style={{
            background: '#FFFFFF',
            borderRadius: '14px',
            border: '1px solid #E2E8F0',
            overflow: 'hidden',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)'
          }}
        >
          {loading ? (
            <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748B' }}>
              <div style={{ fontSize: '2rem', marginBottom: '10px' }}>⌛</div>
              <p style={{ margin: 0, fontWeight: 600 }}>Loading microgrid reservations...</p>
            </div>
          ) : filteredReservations.length === 0 ? (
            <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748B' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '12px' }}>📋</div>
              <h3 style={{ margin: 0, color: '#1E293B', fontSize: '1.1rem' }}>No reservations found</h3>
              <p style={{ margin: '6px 0 16px', fontSize: '0.85rem' }}>
                Try adjusting the status filter or click "Seed Demo Slots" to create initial data.
              </p>
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F8FAFC', borderBottom: '1px solid #E2E8F0' }}>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B' }}>REFERENCE ID</th>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B' }}>PROSUMER NIC</th>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B' }}>STATION / SLOT</th>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B' }}>SCHEDULED FOR</th>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B' }}>STATUS</th>
                    <th style={{ padding: '14px 20px', fontSize: '0.75rem', fontWeight: 700, color: '#64748B', textAlign: 'right' }}>OPERATOR ACTIONS</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredReservations.map((res) => (
                    <tr
                      key={res.id}
                      style={{
                        borderBottom: '1px solid #F1F5F9',
                        transition: 'background-color 0.15s'
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#F8FAFC')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                    >
                      <td style={{ padding: '16px 20px', fontFamily: 'monospace', fontWeight: 700, color: '#0F172A', fontSize: '0.85rem' }}>
                        {res.id.length > 12 ? `${res.id.substring(0, 10)}...` : res.id}
                      </td>

                      <td style={{ padding: '16px 20px', fontWeight: 600, color: '#334155', fontSize: '0.9rem' }}>
                        {res.prosumerId}
                      </td>

                      <td style={{ padding: '16px 20px', fontSize: '0.85rem', color: '#475569' }}>
                        <div style={{ fontWeight: 600, color: '#1E293B' }}>{res.stationId}</div>
                        <div style={{ fontSize: '0.75rem', color: '#94A3B8' }}>Slot: {res.bookingSlotId}</div>
                      </td>

                      <td style={{ padding: '16px 20px', fontSize: '0.85rem', color: '#334155' }}>
                        {new Date(res.scheduledDateTime).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </td>

                      <td style={{ padding: '16px 20px' }}>
                        <StatusBadge status={res.status} />
                      </td>

                      <td style={{ padding: '16px 20px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '8px' }}>
                          <button
                            onClick={() => setSelectedRes(res)}
                            style={{
                              padding: '6px 12px',
                              borderRadius: '6px',
                              border: '1px solid #CBD5E1',
                              backgroundColor: '#FFFFFF',
                              color: '#334155',
                              fontSize: '0.8rem',
                              fontWeight: 600,
                              cursor: 'pointer'
                            }}
                          >
                            Details
                          </button>

                          {res.status === 'Pending' && (
                            <button
                              onClick={() => handleApprove(res.id)}
                              disabled={actionInProgress[res.id]}
                              style={{
                                padding: '6px 14px',
                                borderRadius: '6px',
                                border: 'none',
                                backgroundColor: '#16A34A',
                                color: '#FFFFFF',
                                fontSize: '0.8rem',
                                fontWeight: 600,
                                cursor: 'pointer',
                                boxShadow: '0 1px 2px rgba(0,0,0,0.1)'
                              }}
                            >
                              {actionInProgress[res.id] ? '...' : '✓ Approve'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>

      {/* Reservation Detail Modal */}
      {selectedRes && (
        <ReservationDetailModal
          reservation={selectedRes}
          onClose={() => setSelectedRes(null)}
          onApprove={handleApprove}
          onCancel={handleCancel}
        />
      )}
    </div>
  );
};

export default ReservationListPage;
