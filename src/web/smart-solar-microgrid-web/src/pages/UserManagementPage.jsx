/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Master user directory table with real-time text search, multi-criteria filtering, and lifecycle state management.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '../services/adminService';
import { CreateStaffModal } from '../components/CreateStaffModal';
import { RejectReasonModal } from '../components/RejectReasonModal';
import { UserDetailsModal } from '../components/UserDetailsModal';
import { LocationMapModal } from '../components/LocationMapModal';
import {
  Users,
  Search,
  Filter,
  RotateCcw,
  UserPlus,
  CheckCircle,
  XCircle,
  Eye,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Shield,
  Activity,
  MapPin,
  Calendar
} from 'lucide-react';

export const UserManagementPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(15);

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [isMapModalOpen, setIsMapModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [rejectActionType, setRejectActionType] = useState('Deactivate');

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await adminService.getUsers({
        search: searchQuery,
        role: selectedRole,
        status: selectedStatus,
        page,
        pageSize,
      });
      setUsers(data);
    } catch (err) {
      setError(err.message || 'Failed to load user directory.');
    } finally {
      setLoading(false);
    }
  }, [searchQuery, selectedRole, selectedStatus, page, pageSize]);

  // Debounced search / filter trigger
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchUsers();
    }, 250);
    return () => clearTimeout(timer);
  }, [fetchUsers]);

  const handleClearFilters = () => {
    setSearchQuery('');
    setSelectedRole('ALL');
    setSelectedStatus('ALL');
    setPage(1);
  };

  const handleActivate = async (user) => {
    const id = user.id || user.userId;
    try {
      await adminService.activateUser(id);
      setActionSuccess(`User '${user.username}' activated successfully!`);
      setTimeout(() => setActionSuccess(''), 4000);
      fetchUsers();
    } catch (err) {
      setError(`Activation failed: ${err.message}`);
    }
  };

  const handleDeactivateClick = (user) => {
    setSelectedUser(user);
    setRejectActionType('Deactivate');
    setIsRejectModalOpen(true);
  };

  const handleDeactivateConfirm = async (user, reason) => {
    const id = user.id || user.userId;
    try {
      await adminService.updateUserStatus(id, 'Deactivated', reason);
      setActionSuccess(`User '${user.username}' has been deactivated.`);
      setTimeout(() => setActionSuccess(''), 4000);
      fetchUsers();
    } catch (err) {
      setError(`Deactivation failed: ${err.message}`);
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
        return <span className="badge badge-role-prosumer">Prosumer</span>;
      default:
        return <span className="badge">{role}</span>;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  return (
    <div>
      {/* Title & Action Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem',
        marginBottom: '1.75rem'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
            <Users size={28} style={{ color: 'var(--role-operator)' }} />
            <h1 style={{ fontSize: '1.75rem' }}>System User Directory</h1>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Search, filter, inspect, and manage accounts across all microgrid user roles.
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => setIsCreateModalOpen(true)}
        >
          <UserPlus size={18} />
          <span>Create Staff Account</span>
        </button>
      </div>

      {/* Action Success Alert */}
      {actionSuccess && (
        <div className="alert alert-success">
          <CheckCircle size={18} />
          <span>{actionSuccess}</span>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Search & Filter Bar */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="card-body" style={{ padding: '1.25rem 1.5rem' }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1rem',
            alignItems: 'flex-end'
          }}>
            {/* Search Input */}
            <div className="form-group filter-grid-2-span" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <Search size={14} />
                <span>Search by NIC, Username, Name, or Email</span>
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search..."
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setPage(1);
                  }}
                  style={{ paddingLeft: '2.5rem' }}
                />
                <Search size={16} style={{
                  position: 'absolute',
                  left: '0.85rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-dim)'
                }} />
              </div>
            </div>

            {/* Role Filter */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Role</label>
              <select
                className="form-control"
                value={selectedRole}
                onChange={(e) => {
                  setSelectedRole(e.target.value);
                  setPage(1);
                }}
              >
                <option value="ALL">All Roles</option>
                <option value="Prosumer">Prosumer</option>
                <option value="GridOperator">Grid Operator</option>
                <option value="Backoffice">Backoffice</option>
                <option value="Administrator">Administrator</option>
              </select>
            </div>

            {/* Status Filter */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Account Status</label>
              <select
                className="form-control"
                value={selectedStatus}
                onChange={(e) => {
                  setSelectedStatus(e.target.value);
                  setPage(1);
                }}
              >
                <option value="ALL">All Statuses</option>
                <option value="Active">Active</option>
                <option value="PendingActivation">Pending Approval</option>
                <option value="Deactivated">Deactivated</option>
              </select>
            </div>

            {/* Clear Filters */}
            <div>
              <button
                className="btn btn-outline"
                onClick={handleClearFilters}
                style={{ width: '100%' }}
                title="Reset Search and Filters"
              >
                <RotateCcw size={16} />
                <span>Clear Filters</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* User Directory Table */}
      <div className="table-container overflow-x-auto">
        <table className="data-table whitespace-nowrap">
          <thead>
            <tr>
              <th>NIC</th>
              <th>User / Name</th>
              <th>Role</th>
              <th>Status</th>
              <th>Facility / Address</th>
              <th>Coordinates</th>
              <th>Joined Date</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '3rem' }}>
                  <span className="spinner" style={{ width: '28px', height: '28px', marginBottom: '0.5rem' }}></span>
                  <p style={{ color: 'var(--text-muted)' }}>Querying user directory...</p>
                </td>
              </tr>
            )}

            {!loading && users.length === 0 && (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                  <Users size={36} style={{ color: 'var(--text-dim)', marginBottom: '0.5rem' }} />
                  <p>No user accounts matched your search criteria.</p>
                </td>
              </tr>
            )}

            {!loading && users.map((user) => {
              const id = user.id || user.userId;
              const isPending = user.status === 'PendingActivation';
              const isActive = user.status === 'Active';
              const isDeactivated = user.status === 'Deactivated';

              return (
                <tr key={id}>
                  <td style={{ fontWeight: 600, fontFamily: 'monospace' }}>{user.nic}</td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{user.fullName || user.username}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      @{user.username} {user.email ? `• ${user.email}` : ''}
                    </div>
                  </td>
                  <td>{getRoleBadge(user.role)}</td>
                  <td>{getStatusBadge(user.status)}</td>
                  <td style={{ maxWidth: '200px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.address || <span style={{ color: 'var(--text-dim)' }}>N/A</span>}
                  </td>
                  <td>
                    {user.latitude !== null && user.latitude !== undefined ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem', alignItems: 'flex-start' }}>
                        <span style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--solar-amber)', fontWeight: 600 }}>
                          {user.latitude.toFixed(3)}°, {user.longitude?.toFixed(3)}°
                        </span>
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          style={{
                            padding: '0.15rem 0.45rem',
                            fontSize: '0.7rem',
                            color: 'var(--solar-amber)',
                            borderColor: 'rgba(245, 158, 11, 0.4)',
                            background: 'rgba(245, 158, 11, 0.08)',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem'
                          }}
                          onClick={() => {
                            setSelectedUser(user);
                            setIsMapModalOpen(true);
                          }}
                          title="View on Map"
                        >
                          <MapPin size={11} />
                          <span>Map</span>
                        </button>
                      </div>
                    ) : (
                      <span style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>-</span>
                    )}
                  </td>
                  <td style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                    {formatDate(user.createdAt)}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                      <button
                        className="btn btn-outline btn-sm"
                        onClick={() => {
                          setSelectedUser(user);
                          setIsDetailsModalOpen(true);
                        }}
                        title="View Complete Details"
                      >
                        <Eye size={15} />
                      </button>

                      {isPending && (
                        <button
                          className="btn btn-success btn-sm"
                          onClick={() => handleActivate(user)}
                          title="Approve & Activate"
                        >
                          <CheckCircle size={15} />
                        </button>
                      )}

                      {isActive && (
                        <button
                          className="btn btn-outline btn-sm"
                          style={{ color: 'var(--status-deactivated)', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                          onClick={() => handleDeactivateClick(user)}
                          title="Deactivate User"
                        >
                          <XCircle size={15} />
                        </button>
                      )}

                      {isDeactivated && (
                        <button
                          className="btn btn-outline btn-sm"
                          style={{ color: 'var(--status-active)', borderColor: 'rgba(16, 185, 129, 0.3)' }}
                          onClick={() => handleActivate(user)}
                          title="Reactivate User"
                        >
                          <CheckCircle size={15} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginTop: '1.25rem',
        color: 'var(--text-muted)',
        fontSize: '0.85rem'
      }}>
        <div>
          Showing page <strong>{page}</strong> ({users.length} records loaded)
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            disabled={page === 1 || loading}
          >
            <ChevronLeft size={16} />
            <span>Previous</span>
          </button>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => setPage((p) => p + 1)}
            disabled={users.length < pageSize || loading}
          >
            <span>Next</span>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {/* Modals */}
      <CreateStaffModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onStaffCreated={() => {
          setActionSuccess('Staff account created successfully!');
          fetchUsers();
          setTimeout(() => setActionSuccess(''), 4000);
        }}
      />

      <UserDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => {
          setIsDetailsModalOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
      />

      <RejectReasonModal
        isOpen={isRejectModalOpen}
        onClose={() => {
          setIsRejectModalOpen(false);
          setSelectedUser(null);
        }}
        onConfirm={handleDeactivateConfirm}
        targetUser={selectedUser}
        actionType={rejectActionType}
      />

      <LocationMapModal
        isOpen={isMapModalOpen}
        onClose={() => {
          setIsMapModalOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
      />
    </div>
  );
};
