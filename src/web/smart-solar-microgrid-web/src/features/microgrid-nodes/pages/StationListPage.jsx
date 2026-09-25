/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Master station node management table for Backoffice portal with real-time status and lifecycle actions.
 * Author: Member 2
 */

import React, { useState, useEffect, useCallback } from 'react';
import { stationService } from '../../../services/stationService';
import { StationFormModal } from '../components/StationFormModal';
import { StationDeactivateModal } from '../components/StationDeactivateModal';
import {
  Zap,
  MapPin,
  BatteryCharging,
  Clock,
  RotateCcw,
  Edit,
  Trash2,
  AlertCircle,
  Plus,
  Search,
  CheckCircle2,
  XCircle,
  HelpCircle
} from 'lucide-react';

export const StationListPage = () => {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal States
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingStation, setEditingStation] = useState(null);

  const [isDeactivateOpen, setIsDeactivateOpen] = useState(false);
  const [deactivatingStation, setDeactivatingStation] = useState(null);
  const [deactivateError, setDeactivateError] = useState('');
  const [isDeactivating, setIsDeactivating] = useState(false);

  const fetchStations = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await stationService.getAllStations();
      setStations(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load microgrid stations.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStations();
  }, [fetchStations]);

  // Client-side search and status filter
  const filteredStations = stations.filter((station) => {
    const matchesSearch =
      !searchQuery ||
      station.stationName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      station.location?.address?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus =
      statusFilter === 'ALL' ||
      station.status?.toUpperCase() === statusFilter.toUpperCase();

    return matchesSearch && matchesStatus;
  });

  const handleCreate = () => {
    setEditingStation(null);
    setIsFormOpen(true);
  };

  const handleEdit = (station) => {
    setEditingStation(station);
    setIsFormOpen(true);
  };

  const handleOpenDeactivate = (station) => {
    setDeactivatingStation(station);
    setDeactivateError('');
    setIsDeactivateOpen(true);
  };

  const handleSaveStation = async (payload) => {
    if (payload.id) {
      await stationService.updateStation(payload.id, payload);
    } else {
      await stationService.createStation(payload);
    }
    await fetchStations();
  };

  const handleConfirmDeactivate = async (id) => {
    setIsDeactivating(true);
    setDeactivateError('');
    try {
      await stationService.deactivateStation(id);
      setIsDeactivateOpen(false);
      setDeactivatingStation(null);
      await fetchStations();
    } catch (err) {
      // FAT Business Rule: If 409 Conflict (DEACTIVATION_BLOCKED), extract the server error message
      const errorMessage =
        err.response?.data?.message ||
        err.response?.data?.Message ||
        err.message ||
        'Cannot deactivate node: Active reservations exist.';
      setDeactivateError(errorMessage);
    } finally {
      setIsDeactivating(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Zap size={24} style={{ color: 'var(--solar-amber)' }} />
            <span>Microgrid Node Directory</span>
          </h1>
          <p className="page-subtitle">
            Manage physical solar substation nodes, storage capacity, battery bay slots, and operational trading hours.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button
            className="btn btn-outline btn-sm"
            onClick={fetchStations}
            disabled={loading}
            title="Refresh Station List"
          >
            <RotateCcw size={16} className={loading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            className="btn btn-primary btn-sm"
            onClick={handleCreate}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <Plus size={16} />
            <span>Add New Station</span>
          </button>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '1rem', background: 'var(--bg-card)' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
            <Search
              size={16}
              style={{
                position: 'absolute',
                left: '0.75rem',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted)'
              }}
            />
            <input
              type="text"
              className="form-control"
              placeholder="Search station by name or address..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '2.4rem' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Status:</span>
            <select
              className="form-control"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '140px' }}
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="MAINTENANCE">Maintenance</option>
            </select>
          </div>
        </div>
      </div>

      {/* Station List Table Card */}
      <div className="card">
        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Zap size={18} style={{ color: 'var(--solar-amber)' }} />
            <span style={{ fontWeight: 600 }}>Registered Stations</span>
          </div>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Total Nodes: {filteredStations.length}
          </span>
        </div>

        <div className="table-container">
          <table className="table" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th>Station Name</th>
                <th>Location (GPS)</th>
                <th>Capacity (kWh)</th>
                <th>Battery Bays</th>
                <th>Trading Hours</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
                    <div className="spinner" style={{ margin: '0 auto 0.75rem auto' }} />
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Loading microgrid nodes...</p>
                  </td>
                </tr>
              ) : filteredStations.length === 0 ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
                    <HelpCircle size={32} style={{ color: 'var(--text-muted)', marginBottom: '0.5rem' }} />
                    <p style={{ fontWeight: 600, color: 'var(--text-main)' }}>No microgrid stations found</p>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {searchQuery || statusFilter !== 'ALL'
                        ? 'Try adjusting your search criteria or clear active filters.'
                        : 'No stations have been registered yet. Click "Add New Station" to create the first station.'}
                    </p>
                  </td>
                </tr>
              ) : (
                filteredStations.map((station) => {
                  const isActive = station.status?.toLowerCase() === 'active';
                  const availableBays = station.availableBatterySlots ?? station.batterySlots?.filter(b => b.isAvailable).length ?? 0;
                  const totalBays = station.totalBatterySlots ?? station.batterySlots?.length ?? 0;

                  return (
                    <tr key={station.id}>
                      {/* Station Name & Code */}
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                            {station.stationName}
                          </span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            ID: {station.id?.slice(-8) || 'N/A'}
                          </span>
                        </div>
                      </td>

                      {/* Location Coordinates */}
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', color: 'var(--text-main)' }}>
                            <MapPin size={14} style={{ color: 'var(--solar-amber)' }} />
                            <span>
                              {station.location?.lat?.toFixed(4)}, {station.location?.lng?.toFixed(4)}
                            </span>
                          </div>
                          {station.location?.address && (
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', maxWidth: '220px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {station.location.address}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Capacity */}
                      <td>
                        <span style={{ fontWeight: 600, color: 'var(--solar-amber)' }}>
                          {station.capacityKwh ? station.capacityKwh.toLocaleString() : 0} kWh
                        </span>
                      </td>

                      {/* Available Bays */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <BatteryCharging size={16} style={{ color: availableBays > 0 ? '#10b981' : '#f59e0b' }} />
                          <span style={{ fontWeight: 600, color: availableBays > 0 ? '#10b981' : 'var(--text-muted)' }}>
                            {availableBays} / {totalBays} Free
                          </span>
                        </div>
                      </td>

                      {/* Trading Hours */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                          <Clock size={14} />
                          <span>
                            {station.schedule?.openTime || '06:00'} - {station.schedule?.closeTime || '20:00'}
                          </span>
                        </div>
                      </td>

                      {/* Status Badge */}
                      <td>
                        <span
                          className={`badge ${isActive ? 'badge-active' : 'badge-deactivated'}`}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.3rem',
                            fontSize: '0.75rem',
                            padding: '0.25rem 0.6rem'
                          }}
                        >
                          {isActive ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
                          <span>{station.status || 'Active'}</span>
                        </span>
                      </td>

                      {/* Action Buttons */}
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.4rem', justifyContent: 'flex-end' }}>
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleEdit(station)}
                            title="Edit Station Details"
                            style={{ padding: '0.35rem 0.5rem' }}
                          >
                            <Edit size={14} />
                          </button>

                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleOpenDeactivate(station)}
                            title="Deactivate Station Node"
                            disabled={!isActive}
                            style={{
                              padding: '0.35rem 0.5rem',
                              color: isActive ? '#ef4444' : 'var(--text-dim)',
                              borderColor: isActive ? 'rgba(239, 68, 68, 0.3)' : 'var(--border-subtle)'
                            }}
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create / Edit Station Modal */}
      <StationFormModal
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setEditingStation(null);
        }}
        onSave={handleSaveStation}
        initialData={editingStation}
      />

      {/* Station Deactivation Confirmation Modal with FAT Rule Error Display */}
      <StationDeactivateModal
        isOpen={isDeactivateOpen}
        onClose={() => {
          setIsDeactivateOpen(false);
          setDeactivatingStation(null);
          setDeactivateError('');
        }}
        onConfirm={handleConfirmDeactivate}
        station={deactivatingStation}
        isLoading={isDeactivating}
        error={deactivateError}
      />
    </div>
  );
};
