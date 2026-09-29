/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Modal form dialog for creating and updating solar microgrid station nodes with interactive Leaflet GPS Map Picker.
 * Author: Member 2
 */

import React, { useState, useEffect, useRef } from 'react';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import {
  X,
  Zap,
  MapPin,
  BatteryCharging,
  Clock,
  Save,
  AlertCircle,
  Compass,
  Crosshair,
  UserCheck
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

/**
 * Creates custom amber solar pin icon for Leaflet map without broken asset dependencies.
 */
const createPinIcon = () => {
  return L.divIcon({
    className: 'solar-station-pin',
    html: `
      <div style="position: relative; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;">
        <div style="
          width: 24px;
          height: 24px;
          background: #f59e0b;
          border-radius: 50% 50% 50% 0;
          transform: rotate(-45deg);
          border: 2px solid #ffffff;
          box-shadow: 0 0 12px rgba(245, 158, 11, 0.8);
          display: flex;
          align-items: center;
          justify-content: center;
        ">
          <div style="width: 8px; height: 8px; background: #0f172a; border-radius: 50%;"></div>
        </div>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 28]
  });
};

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
    closeTime: '20:00',
    status: 'Active',
    assignedOperatorId: ''
  });

  const [operators, setOperators] = useState([]);
  const [loadingOperators, setLoadingOperators] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showMapPicker, setShowMapPicker] = useState(false);

  // Fetch active Grid Operators when modal opens
  useEffect(() => {
    if (isOpen) {
      setLoadingOperators(true);
      adminService.getUsers({ role: 'GridOperator', status: 'Active', pageSize: 100 })
        .then((data) => {
          setOperators(Array.isArray(data) ? data : []);
        })
        .catch((err) => {
          console.error('Failed to load active grid operators:', err);
        })
        .finally(() => {
          setLoadingOperators(false);
        });
    }
  }, [isOpen]);

  // Map references
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerInstanceRef = useRef(null);

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
        closeTime: initialData.schedule?.closeTime || '20:00',
        status: initialData.status || 'Active',
        assignedOperatorId: initialData.assignedOperatorId || ''
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
        closeTime: '20:00',
        status: 'Active',
        assignedOperatorId: ''
      });
    }
    setError('');
  }, [initialData, isOpen]);

  // Teardown map instance when modal closes
  useEffect(() => {
    if (!isOpen) {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markerInstanceRef.current = null;
      }
      setShowMapPicker(false);
    }
  }, [isOpen]);

  // Initialize and synchronize Leaflet Map Picker
  useEffect(() => {
    if (!isOpen || !showMapPicker || !mapContainerRef.current) return;

    const currentLat = parseFloat(formData.lat) || 6.9271;
    const currentLng = parseFloat(formData.lng) || 79.8612;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [currentLat, currentLng],
        zoom: 13,
        zoomControl: true
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
      }).addTo(map);

      const marker = L.marker([currentLat, currentLng], {
        icon: createPinIcon(),
        draggable: true
      }).addTo(map);

      marker.on('dragend', (e) => {
        const position = e.target.getLatLng();
        setFormData((prev) => ({
          ...prev,
          lat: parseFloat(position.lat.toFixed(6)),
          lng: parseFloat(position.lng.toFixed(6))
        }));
      });

      map.on('click', (e) => {
        const { lat, lng } = e.latlng;
        marker.setLatLng([lat, lng]);
        setFormData((prev) => ({
          ...prev,
          lat: parseFloat(lat.toFixed(6)),
          lng: parseFloat(lng.toFixed(6))
        }));
      });

      mapInstanceRef.current = map;
      markerInstanceRef.current = marker;
    } else {
      mapInstanceRef.current.setView([currentLat, currentLng], 13);
      if (markerInstanceRef.current) {
        markerInstanceRef.current.setLatLng([currentLat, currentLng]);
      }
    }

    const timer = setTimeout(() => {
      mapInstanceRef.current?.invalidateSize();
    }, 200);

    return () => {
      clearTimeout(timer);
    };
  }, [showMapPicker, isOpen]);

  // Sync marker position if manual input changes
  useEffect(() => {
    if (mapInstanceRef.current && markerInstanceRef.current) {
      const latVal = parseFloat(formData.lat);
      const lngVal = parseFloat(formData.lng);
      if (!isNaN(latVal) && !isNaN(lngVal) && latVal >= -90 && latVal <= 90 && lngVal >= -180 && lngVal <= 180) {
        markerInstanceRef.current.setLatLng([latVal, lngVal]);
      }
    }
  }, [formData.lat, formData.lng]);

  const handleGetCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const newLat = parseFloat(position.coords.latitude.toFixed(6));
          const newLng = parseFloat(position.coords.longitude.toFixed(6));
          setFormData((prev) => ({
            ...prev,
            lat: newLat,
            lng: newLng
          }));
          if (mapInstanceRef.current && markerInstanceRef.current) {
            mapInstanceRef.current.flyTo([newLat, newLng], 14);
            markerInstanceRef.current.setLatLng([newLat, newLng]);
          }
        },
        () => {
          setError('Unable to retrieve current browser GPS position.');
        }
      );
    }
  };

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

    if (!formData.assignedOperatorId) {
      setError('Please assign an active Grid Operator to this microgrid station.');
      return;
    }

    // Prepare payload matching StationCreateDto / StationUpdateDto
    const payload = {
      ...(isEditing && { id: initialData.id, status: formData.status }),
      stationName: formData.stationName.trim(),
      assignedOperatorId: formData.assignedOperatorId,
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
        style={{ maxWidth: '680px', maxHeight: '92vh', overflowY: 'auto' }}
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

            {/* Station Name & Status (in Edit Mode) */}
            <div className={isEditing ? "responsive-grid-edit-status" : ""}>
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

              {isEditing && (
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" style={{ fontWeight: 600 }}>
                    Station Status
                  </label>
                  <select
                    name="status"
                    className="form-control"
                    value={formData.status}
                    onChange={handleChange}
                  >
                    <option value="Active">Active</option>
                    <option value="Inactive">Inactive</option>
                    <option value="Maintenance">Maintenance</option>
                  </select>
                </div>
              )}
            </div>

            {/* Location (Lat, Lng) with 'Select on Map' Trigger */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem', margin: 0 }}>
                  <MapPin size={16} style={{ color: 'var(--solar-amber)' }} />
                  <span>GPS Location Coordinates <span style={{ color: '#ef4444' }}>*</span></span>
                </label>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={() => setShowMapPicker(!showMapPicker)}
                  style={{
                    fontSize: '0.75rem',
                    padding: '0.2rem 0.6rem',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    borderColor: showMapPicker ? 'var(--solar-amber)' : 'var(--border-subtle)',
                    color: showMapPicker ? 'var(--solar-amber)' : 'var(--text-main)',
                    background: showMapPicker ? 'rgba(245, 158, 11, 0.1)' : 'transparent'
                  }}
                  title="Toggle Interactive GPS Map Picker"
                >
                  <Compass size={14} />
                  <span>{showMapPicker ? 'Hide Map Picker' : 'Select on Map'}</span>
                </button>
              </div>

              {/* Collapsible Interactive Leaflet Map Picker Canvas */}
              {showMapPicker && (
                <div
                  style={{
                    marginBottom: '0.75rem',
                    border: '1px solid var(--border-light)',
                    borderRadius: 'var(--radius-md)',
                    overflow: 'hidden',
                    background: '#0f172a'
                  }}
                >
                  <div
                    style={{
                      padding: '0.4rem 0.75rem',
                      background: 'var(--bg-card-header)',
                      borderBottom: '1px solid var(--border-subtle)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      fontSize: '0.75rem'
                    }}
                  >
                    <span style={{ color: 'var(--solar-amber)', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <Crosshair size={13} />
                      Click anywhere on the map or drag the pin to set GPS coordinates
                    </span>
                    <button
                      type="button"
                      onClick={handleGetCurrentLocation}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--text-main)',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.25rem',
                        fontSize: '0.75rem',
                        textDecoration: 'underline'
                      }}
                    >
                      Locate Me
                    </button>
                  </div>
                  <div
                    ref={mapContainerRef}
                    style={{
                      height: '240px',
                      width: '100%',
                      zIndex: 1
                    }}
                  />
                </div>
              )}

              <div className="responsive-grid-2">
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

            {/* Assigned Grid Operator Selection */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <UserCheck size={16} style={{ color: '#10b981' }} />
                <span>Assigned Grid Operator <span style={{ color: '#ef4444' }}>*</span></span>
              </label>
              <select
                name="assignedOperatorId"
                className="form-control"
                value={formData.assignedOperatorId}
                onChange={handleChange}
                required
              >
                <option value="">-- Select Active Grid Operator --</option>
                {operators.map((op) => (
                  <option key={op.id || op.userId} value={op.id || op.userId}>
                    {op.fullName} ({op.nic}) - {op.email}
                  </option>
                ))}
              </select>
              {loadingOperators && (
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem', display: 'block' }}>
                  Loading active grid operators...
                </span>
              )}
            </div>

            {/* Capacity & Battery Slots */}
            <div className="responsive-grid-2">
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
              <div className="responsive-grid-2">
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
