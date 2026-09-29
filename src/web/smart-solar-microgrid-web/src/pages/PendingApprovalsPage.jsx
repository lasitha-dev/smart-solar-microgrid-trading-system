/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Prosumer registration approval dashboard with separate facility address and coordinates columns, interactive map viewer, and live polling.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import { adminService } from '../services/adminService';
import { RejectReasonModal } from '../components/RejectReasonModal';
import { UserDetailsModal } from '../components/UserDetailsModal';
import { LocationMapModal } from '../components/LocationMapModal';
import {
  UserCheck,
  CheckCircle2,
  XCircle,
  Phone,
  AlertCircle,
  Eye,
  Shield,
  Radio,
  Sun,
  MapPin,
  Sparkles
} from 'lucide-react';

export const PendingApprovalsPage = () => {
  const { refreshPendingCount } = useOutletContext() || {};
  const [pendingProsumers, setPendingProsumers] = useState([]);
  const [userStats, setUserStats] = useState({
    activeProsumers: 0,
    gridOperators: 0,
    backofficeOfficers: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [processingId, setProcessingId] = useState(null);

  // Modals state
  const [selectedUser, setSelectedUser] = useState(null);
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const [isRejectOpen, setIsRejectOpen] = useState(false);
  const [isMapOpen, setIsMapOpen] = useState(false);

  // Fetch pending prosumers and system stats
  const fetchData = useCallback(async (isBackground = false) => {
    if (!isBackground && pendingProsumers.length === 0) {
      setLoading(true);
    }
    try {
      const [pendingData, allUsersData] = await Promise.all([
        adminService.getPendingProsumers(),
        adminService.getUsers({ pageSize: 1000 }),
      ]);

      setPendingProsumers(pendingData);

      // Compute counts
      const activePros = (allUsersData || []).filter(
        (u) => u.role === 'Prosumer' && u.status === 'Active'
      ).length;
      const gridOps = (allUsersData || []).filter(
        (u) => u.role === 'GridOperator'
      ).length;
      const backoffice = (allUsersData || []).filter(
        (u) => u.role === 'Backoffice' || u.role === 'Administrator'
      ).length;

      setUserStats({
        activeProsumers: activePros,
        gridOperators: gridOps,
        backofficeOfficers: backoffice,
      });

      if (refreshPendingCount) refreshPendingCount();
      setError('');
    } catch (err) {
      if (!isBackground) {
        setError(err.message || 'Failed to fetch pending prosumer registrations.');
      }
    } finally {
      if (!isBackground) {
        setLoading(false);
      }
    }
  }, [pendingProsumers.length, refreshPendingCount]);

  // Initial load + Real-time automatic polling every 3.5s
  useEffect(() => {
    fetchData(false);
    const interval = setInterval(() => {
      fetchData(true);
    }, 3500);
    return () => clearInterval(interval);
  }, [fetchData]);

  const handleApprove = async (prosumer) => {
    const id = prosumer.id || prosumer.userId;
    setProcessingId(id);
    setError('');

    // Optimistic UI update
    const previousList = [...pendingProsumers];
    setPendingProsumers((prev) => prev.filter((p) => (p.id || p.userId) !== id));
    setUserStats((prev) => ({
      ...prev,
      activeProsumers: prev.activeProsumers + 1,
    }));

    try {
      await adminService.updateUserStatus(id, 'Active');
      setSuccessMsg(`Prosumer '${prosumer.fullName || prosumer.username}' successfully approved and activated!`);
      if (refreshPendingCount) refreshPendingCount();
      fetchData(true);
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (err) {
      setPendingProsumers(previousList);
      setError(`Approval failed: ${err.message}`);
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectConfirm = async (prosumer, reason) => {
    const id = prosumer.id || prosumer.userId;
    setProcessingId(id);
    setError('');

    // Optimistic UI update
    const previousList = [...pendingProsumers];
    setPendingProsumers((prev) => prev.filter((p) => (p.id || p.userId) !== id));

    try {
      await adminService.updateUserStatus(id, 'Deactivated', reason);
      setSuccessMsg(`Prosumer registration for '${prosumer.fullName || prosumer.username}' has been rejected.`);
      if (refreshPendingCount) refreshPendingCount();
      fetchData(true);
      setTimeout(() => setSuccessMsg(''), 5000);
    } catch (err) {
      setPendingProsumers(previousList);
      setError(`Rejection failed: ${err.message}`);
    } finally {
      setProcessingId(null);
    }
  };

  const handleViewMap = (prosumer) => {
    setSelectedUser(prosumer);
    setIsMapOpen(true);
  };

  return (
    <div>
      {/* Header */}
      <div style={{
        marginBottom: '1.75rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
          <UserCheck size={28} style={{ color: 'var(--solar-amber)' }} />
          <h1 style={{ fontSize: '1.75rem' }}>Pending Prosumers Approval</h1>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Verify registered solar prosumers, inspect facility coordinates on map, and grant microgrid trading access.
        </p>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="alert alert-success">
          <CheckCircle2 size={18} />
          <span>{successMsg}</span>
        </div>
      )}

      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* 4 Summary Stat Cards */}
      <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        {/* Card 1: Pending Approvals */}
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--status-pending-bg)', color: 'var(--status-pending)' }}>
            <UserCheck size={24} />
          </div>
          <div>
            <div className="stat-val" style={{ color: 'var(--solar-amber)' }}>{pendingProsumers.length}</div>
            <div className="stat-lbl">Pending Approvals</div>
          </div>
        </div>

        {/* Card 2: Active Prosumers */}
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--status-active-bg)', color: 'var(--status-active)' }}>
            <Sun size={24} />
          </div>
          <div>
            <div className="stat-val" style={{ color: 'var(--status-active)' }}>{userStats.activeProsumers}</div>
            <div className="stat-lbl">Active Prosumers</div>
          </div>
        </div>

        {/* Card 3: Grid Operators */}
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--role-operator-bg)', color: 'var(--role-operator)' }}>
            <Radio size={24} />
          </div>
          <div>
            <div className="stat-val" style={{ color: 'var(--role-operator)' }}>{userStats.gridOperators}</div>
            <div className="stat-lbl">Grid Operators</div>
          </div>
        </div>

        {/* Card 4: Backoffice Officers */}
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--role-backoffice-bg)', color: 'var(--role-backoffice)' }}>
            <Shield size={24} />
          </div>
          <div>
            <div className="stat-val" style={{ color: 'var(--role-backoffice)' }}>{userStats.backofficeOfficers}</div>
            <div className="stat-lbl">Backoffice Officers</div>
          </div>
        </div>
      </div>

      {/* Loading Skeleton */}
      {loading && pendingProsumers.length === 0 && (
        <div style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--solar-amber)' }}>
          <span className="spinner" style={{ width: '32px', height: '32px', marginBottom: '1rem' }}></span>
          <p style={{ color: 'var(--text-muted)' }}>Querying pending prosumer registrations from MongoDB...</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && pendingProsumers.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            background: 'var(--status-active-bg)',
            color: 'var(--status-active)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1rem'
          }}>
            <Sparkles size={32} />
          </div>
          <h3 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>All Caught Up!</h3>
          <p style={{ color: 'var(--text-muted)', maxWidth: '400px', margin: '0 auto', fontSize: '0.9rem' }}>
            There are no pending prosumer registrations awaiting approval at this time. New registrations will automatically appear here in real-time.
          </p>
        </div>
      )}

      {/* Single Clean Table View with Separate Facility Address & Coordinates Columns */}
      {pendingProsumers.length > 0 && (
        <div className="table-container overflow-x-auto">
          <table className="data-table whitespace-nowrap">
            <thead>
              <tr>
                <th>Prosumer Details</th>
                <th>NIC</th>
                <th>Contact</th>
                <th>Facility Address</th>
                <th>Coordinates</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {pendingProsumers.map((prosumer) => {
                const id = prosumer.id || prosumer.userId;
                const isProcessing = processingId === id;
                const lat = prosumer.latitude;
                const lon = prosumer.longitude;
                const hasCoordinates = lat !== null && lat !== undefined && lon !== null && lon !== undefined;

                return (
                  <tr key={id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{prosumer.fullName || prosumer.username}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>@{prosumer.username}</div>
                    </td>
                    <td style={{ fontWeight: 600, fontFamily: 'monospace' }}>{prosumer.nic}</td>
                    <td>
                      <div>{prosumer.phone || 'N/A'}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{prosumer.email || 'N/A'}</div>
                    </td>
                    <td style={{ maxWidth: '220px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {prosumer.address || <span style={{ color: 'var(--text-dim)' }}>Address Not Provided</span>}
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', alignItems: 'flex-start' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--solar-amber)', fontFamily: 'monospace', fontWeight: 600 }}>
                          {hasCoordinates ? `${lat.toFixed(4)}°, ${lon.toFixed(4)}°` : '6.9271°, 79.8612°'}
                        </div>
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          style={{
                            padding: '0.2rem 0.55rem',
                            fontSize: '0.725rem',
                            color: 'var(--solar-amber)',
                            borderColor: 'rgba(245, 158, 11, 0.4)',
                            background: 'rgba(245, 158, 11, 0.08)',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.3rem'
                          }}
                          onClick={() => handleViewMap(prosumer)}
                          title="View on Interactive Map"
                        >
                          <MapPin size={12} />
                          <span>View on Map</span>
                        </button>
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-pending">Pending</span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.5rem' }}>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => {
                            setSelectedUser(prosumer);
                            setIsDetailsOpen(true);
                          }}
                          title="Inspect Full Profile"
                        >
                          <Eye size={15} />
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => {
                            setSelectedUser(prosumer);
                            setIsRejectOpen(true);
                          }}
                          disabled={isProcessing}
                          title="Reject Registration"
                        >
                          <XCircle size={15} />
                        </button>
                        <button
                          className="btn btn-success btn-sm"
                          onClick={() => handleApprove(prosumer)}
                          disabled={isProcessing}
                          title="Approve & Activate"
                        >
                          {isProcessing ? <span className="spinner"></span> : <CheckCircle2 size={15} />}
                          <span>Approve</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Inspector Modal */}
      <UserDetailsModal
        isOpen={isDetailsOpen}
        onClose={() => {
          setIsDetailsOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
      />

      {/* Reject Modal */}
      <RejectReasonModal
        isOpen={isRejectOpen}
        onClose={() => {
          setIsRejectOpen(false);
          setSelectedUser(null);
        }}
        onConfirm={handleRejectConfirm}
        targetUser={selectedUser}
        actionType="Reject"
      />

      {/* Interactive Map Modal */}
      <LocationMapModal
        isOpen={isMapOpen}
        onClose={() => {
          setIsMapOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
      />
    </div>
  );
};
