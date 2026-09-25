/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Modal form dialog for creating and updating solar microgrid station nodes with validation.
 * Author: Member 2
 */

import React, { useState, useEffect } from 'react';
import {
  X,
  Zap,
  MapPin,
  BatteryCharging,
  Clock,
  Save,
  AlertCircle
} from 'lucide-react';

export const StationFormModal = ({ isOpen, onClose, onSave, initialData = null }) => {
  const isEditing = Boolean(initialData?.id);

  const [formData, setFormData] = useState({
    stationName: '',
    lat: 6.9271,
    lng: 79.8612,
    address: '',
    capacityKwh: 100,
    totalBatterySlots: 4,
    openTime: '06:00',
    closeTime: '20:00'
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialData) {
      setFormData({
        stationName: initialData.stationName || '',
        lat: initialData.location?.lat ?? 6.9271,
        lng: initialData.location?.lng ?? 79.8612,
        address: initialData.location?.address || '',
        capacityKwh: initialData.capacityKwh ?? 100,
        totalBatterySlots: initialData.totalBatterySlots ?? initialData.batterySlots?.length ?? 4,
        openTime: initialData.schedule?.openTime || '06:00',
        closeTime: initialData.schedule?.closeTime || '20:00'
      });
    } else {
      setFormData({
        stationName: '',
        lat: 6.9271,
        lng: 79.8612,
        address: '',
        capacityKwh: 100,
        totalBatterySlots: 4,
        openTime: '06:00',
        closeTime: '20:00'
      });
    }
    setError('');
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'number' ? (value === '' ? '' : parseFloat(value)) : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Client-side validations
    if (!formData.stationName.trim()) {
      setError('Station name is required.');
      return;
    }

    const latVal = parseFloat(formData.lat);
    const lngVal = parseFloat(formData.lng);
    if (isNaN(latVal) || latVal < -90 || latVal > 90) {
      setError('Latitude must be a valid number between -90 and 90.');
      return;
    }
    if (isNaN(lngVal) || lngVal < -180 || lngVal > 180) {
      setError('Longitude must be a valid number between -180 and 180.');
      return;
    }

    const capacityVal = parseFloat(formData.capacityKwh);
    if (isNaN(capacityVal) || capacityVal <= 0) {
      setError('Capacity must be a positive number greater than 0 kWh.');
      return;
    }

    const slotsVal = parseInt(formData.totalBatterySlots, 10);
    if (isNaN(slotsVal) || slotsVal < 1 || slotsVal > 500) {
      setError('Total battery slots must be between 1 and 500.');
      return;
    }

    // Prepare payload matching StationCreateDto / StationUpdateDto
    const payload = {
      ...(isEditing && { id: initialData.id }),
      stationName: formData.stationName.trim(),
      location: {
        lat: latVal,
        lng: lngVal,
        address: formData.address.trim()
      },
      capacityKwh: capacityVal,
      totalBatterySlots: slotsVal,
      schedule: {
        openTime: formData.openTime || '06:00',
        closeTime: formData.closeTime || '20:00',
        daysActive: initialData?.schedule?.daysActive || [
          'Monday',
          'Tuesday',
          'Wednesday',
          'Thursday',
          'Friday',
          'Saturday',
          'Sunday'
        ]
      }
    };

    setLoading(true);
    try {
      await onSave(payload);
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to save microgrid station.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        style={{ maxWidth: '640px', maxHeight: '92vh', overflowY: 'auto' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                background: 'rgba(245, 158, 11, 0.15)',
                color: 'var(--solar-amber)',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <Zap size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem' }}>
                {isEditing ? 'Edit Microgrid Station' : 'Register New Station Node'}
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {isEditing
                  ? `Updating configuration for node ID: ${initialData.id?.slice(-8)}`
                  : 'Define physical substation node attributes, capacity, and operational hours.'}
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

        {/* Modal Body / Form */}
        <form onSubmit={handleSubmit}>
          <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {error && (
              <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <AlertCircle size={18} />
                <span style={{ fontSize: '0.85rem' }}>{error}</span>
              </div>
            )}

            {/* Station Name */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontWeight: 600 }}>
                Station Node Name <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                type="text"
                name="stationName"
                className="form-control"
                placeholder="e.g. Colombo Fort Solar Hub"
                value={formData.stationName}
                onChange={handleChange}
                required
              />
            </div>

            {/* Location (Lat, Lng) */}
            <div>
              <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <MapPin size={16} style={{ color: 'var(--solar-amber)' }} />
                <span>GPS Location Coordinates <span style={{ color: '#ef4444' }}>*</span></span>
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Latitude (-90 to +90)</label>
                  <input
                    type="number"
                    step="0.000001"
                    name="lat"
                    className="form-control"
                    placeholder="6.9271"
                    value={formData.lat}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Longitude (-180 to +180)</label>
                  <input
                    type="number"
                    step="0.000001"
                    name="lng"
                    className="form-control"
                    placeholder="79.8612"
                    value={formData.lng}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>
            </div>

            {/* Address */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontSize: '0.85rem' }}>
                Physical Address / Landmark
              </label>
              <input
                type="text"
                name="address"
                className="form-control"
                placeholder="e.g. Lotus Road, Fort, Colombo 01"
                value={formData.address}
                onChange={handleChange}
              />
            </div>

            {/* Capacity & Battery Slots */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Zap size={16} style={{ color: 'var(--solar-amber)' }} />
                  <span>Capacity (kWh) <span style={{ color: '#ef4444' }}>*</span></span>
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0.1"
                  name="capacityKwh"
                  className="form-control"
                  placeholder="100.0"
                  value={formData.capacityKwh}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <BatteryCharging size={16} style={{ color: '#10b981' }} />
                  <span>Total Battery Slots <span style={{ color: '#ef4444' }}>*</span></span>
                </label>
                <input
                  type="number"
                  min="1"
                  max="500"
                  name="totalBatterySlots"
                  className="form-control"
                  placeholder="4"
                  value={formData.totalBatterySlots}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            {/* Operating Hours (OpenTime, CloseTime) */}
            <div>
              <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Clock size={16} style={{ color: 'var(--solar-amber)' }} />
                <span>Daily Operating Trading Hours (24H Format)</span>
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Open Time (HH:mm)</label>
                  <input
                    type="time"
                    name="openTime"
                    className="form-control"
                    value={formData.openTime}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Close Time (HH:mm)</label>
                  <input
                    type="time"
                    name="closeTime"
                    className="form-control"
                    value={formData.closeTime}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>
            </div>
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
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              {loading ? <span className="spinner" /> : <Save size={16} />}
              <span>{loading ? 'Saving...' : isEditing ? 'Update Station' : 'Create Station'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
