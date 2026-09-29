/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Prosumer-facing Web Booking Page with 7-day calendar restriction, dynamic station fetching, and slot booking.
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { reservationService } from '../../services/reservationService';
import { stationService } from '../../services/stationService';
import { useAuth } from '../../context/AuthContext';

const FALLBACK_STATIONS = [
  { id: '60d5ec49f1b2c42d8c3b4a59', name: 'Station A – Solar Bay (Main Microgrid)' },
  { id: '60d5ec49f1b2c42d8c3b4a60', name: 'Station B – North Grid (Substation)' }
];

const SlotBookingPage = () => {
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  const todayStr = new Date().toISOString().split('T')[0];
  const maxDate = new Date();
  maxDate.setDate(maxDate.getDate() + 7);
  const maxDateStr = maxDate.toISOString().split('T')[0];

  const [stations, setStations] = useState(FALLBACK_STATIONS);
  const [loadingStations, setLoadingStations] = useState(true);
  const [stationId, setStationId] = useState('');
  const [selectedDate, setSelectedDate] = useState(todayStr);
  const [prosumerNic, setProsumerNic] = useState(user?.nic || user?.nationalId || '200112345678');
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBooking, setSuccessBooking] = useState(null);

  // Fetch registered stations and auto-select first available station
  useEffect(() => {
    const loadStations = async () => {
      try {
        setLoadingStations(true);
        const fetched = await stationService.getAllStations();
        const activeStations = Array.isArray(fetched)
          ? fetched.filter(s => s.status !== 'Deactivated')
          : [];

        if (activeStations.length > 0) {
          setStations(activeStations);
          const paramStationId = searchParams.get('stationId');
          // Automatically select the first available station if none was passed via URL/Map intent
          if (paramStationId && activeStations.some(s => s.id === paramStationId)) {
            setStationId(paramStationId);
          } else {
            setStationId(activeStations[0].id);
          }
        } else {
          setStations(FALLBACK_STATIONS);
          setStationId(FALLBACK_STATIONS[0].id);
        }
      } catch (err) {
        console.warn('Failed to fetch stations, falling back to default:', err);
        setStations(FALLBACK_STATIONS);
        setStationId(FALLBACK_STATIONS[0].id);
      } finally {
        setLoadingStations(false);
      }
    };
    loadStations();
  }, [searchParams]);

  useEffect(() => {
    if (stationId) {
      fetchSlots();
    }
  }, [stationId, selectedDate]);

  const fetchSlots = async () => {
    if (!stationId) return;
    setLoadingSlots(true);
    setErrorBanner(null);
    setSelectedSlot(null);
    try {
      const res = await reservationService.getAvailableSlots(stationId, selectedDate);
      if (res.success) {
        setSlots(res.data || []);
      } else {
        setErrorBanner(res.message);
      }
    } catch (err) {
      setErrorBanner(err.message || 'Failed to retrieve available slots for selected station and date.');
    } finally {
      setLoadingSlots(false);
    }
  };

  const handleBook = async (e) => {
    e.preventDefault();
    if (!selectedSlot) {
      setErrorBanner('Please select an open energy slot from the grid below.');
      return;
    }
    if (!prosumerNic.trim()) {
      setErrorBanner('Please enter your National Identity Card (NIC) number.');
      return;
    }

    setSubmitting(true);
    setErrorBanner(null);

    const scheduledDateTime = new Date(`${selectedDate}T${selectedSlot.startTime}`);

    const dto = {
      prosumerId: prosumerNic.trim(),
      stationId,
      bookingSlotId: selectedSlot.id,
      scheduledDateTime: scheduledDateTime.toISOString()
    };

    try {
      const res = await reservationService.createReservation(dto);
      if (res.success) {
        setSuccessBooking(res.data);
      } else {
        setErrorBanner(res.message || 'Booking rejected by microgrid rules.');
      }
    } catch (err) {
      setErrorBanner(err.message || 'Error occurred during reservation.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ padding: '24px 20px', maxWidth: '850px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: '28px', textAlign: 'center' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', background: 'rgba(16, 185, 129, 0.15)', padding: '6px 14px', borderRadius: '9999px', color: 'var(--status-active)', fontWeight: 700, fontSize: '0.8rem', marginBottom: '8px' }}>
          <span>⚡</span> PROSUMER ENERGY RESERVATION
        </div>
        <h1 style={{ margin: 0, fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
          Reserve Solar Microgrid Charging Slot
        </h1>
        <p style={{ margin: '8px 0 0', color: 'var(--text-muted)', fontSize: '0.95rem' }}>
          Secure clean energy slots in advance up to 7 days ahead.
        </p>
      </div>

      {/* Success Modal / Banner */}
      {successBooking && (
        <div
          style={{
            backgroundColor: 'rgba(16, 185, 129, 0.12)',
            border: '2px solid var(--status-active)',
            borderRadius: '16px',
            padding: '24px',
            marginBottom: '28px',
            boxShadow: '0 4px 6px -1px rgba(16, 185, 129, 0.2)',
            textAlign: 'center'
          }}
        >
          <span style={{ fontSize: '3rem' }}>🎉</span>
          <h2 style={{ margin: '8px 0', color: 'var(--status-active)', fontSize: '1.4rem', fontWeight: 800 }}>
            Reservation Successfully Confirmed!
          </h2>
          <p style={{ color: 'var(--text-muted)', margin: '4px 0 16px' }}>
            Your energy slot has been reserved. Reference ID:
          </p>
          <div style={{ display: 'inline-block', backgroundColor: 'var(--bg-surface)', padding: '10px 20px', borderRadius: '8px', border: '1px dashed var(--status-active)', fontFamily: 'monospace', fontWeight: 800, fontSize: '1.1rem', color: 'var(--solar-amber)' }}>
            {successBooking.id}
          </div>
          <p style={{ marginTop: '16px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Status: <strong>Pending Operator Approval</strong>. You can view this booking anytime in your mobile app.
          </p>
          <button
            onClick={() => {
              setSuccessBooking(null);
              fetchSlots();
            }}
            className="btn btn-success"
            style={{ marginTop: '12px' }}
          >
            Book Another Slot
          </button>
        </div>
      )}

      {/* Error Alert */}
      {errorBanner && (
        <div className="alert alert-danger">
          <span>⚠️</span>
          <div>{errorBanner}</div>
        </div>
      )}

      {/* Booking Form Card */}
      <div className="card">
        <div className="card-body">
          <form onSubmit={handleBook}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px', marginBottom: '24px' }}>
              {/* Station Selection */}
              <div>
                <label className="form-label">
                  Select Microgrid Station
                </label>
                <select
                  value={stationId}
                  onChange={(e) => setStationId(e.target.value)}
                  className="form-control"
                  disabled={loadingStations}
                >
                  {stations.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} {s.locationAddress ? `(${s.locationAddress})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Date Selection with 7-day rule */}
              <div>
                <label className="form-label">
                  Target Date (7-Day Limit)
                </label>
                <input
                  type="date"
                  value={selectedDate}
                  min={todayStr}
                  max={maxDateStr}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="form-control"
                />
              </div>
            </div>

            {/* Prosumer NIC */}
            <div style={{ marginBottom: '24px' }}>
              <label className="form-label">
                Prosumer National Identity Card (NIC)
              </label>
              <input
                type="text"
                placeholder="e.g. 200112345678 or 981234567V"
                value={prosumerNic}
                onChange={(e) => setProsumerNic(e.target.value)}
                className="form-control"
              />
            </div>

            {/* Slots Grid */}
            <div style={{ marginBottom: '24px' }}>
              <label className="form-label" style={{ marginBottom: '8px' }}>
                Available Battery Slots for {selectedDate}
              </label>

              {loadingSlots ? (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  <div className="spinner" style={{ marginRight: '8px' }}></div>
                  Loading energy slots...
                </div>
              ) : slots.length === 0 ? (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-dim)', border: '1px dashed var(--border-light)', borderRadius: '8px' }}>
                  No slots configured for this date. (Use "Seed Demo Slots" on the Operator portal if database is empty).
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '12px' }}>
                  {slots.map((slot) => {
                    const isOpen = slot.status === 'Open';
                    const isSelected = selectedSlot?.id === slot.id;

                    return (
                      <div
                        key={slot.id}
                        onClick={() => isOpen && setSelectedSlot(slot)}
                        style={{
                          padding: '14px',
                          borderRadius: '10px',
                          border: isSelected
                            ? '2px solid var(--status-active)'
                            : isOpen
                            ? '1px solid var(--border-light)'
                            : '1px solid var(--border-subtle)',
                          backgroundColor: isSelected
                            ? 'rgba(16, 185, 129, 0.15)'
                            : isOpen
                            ? 'var(--bg-surface)'
                            : 'var(--bg-main)',
                          cursor: isOpen ? 'pointer' : 'not-allowed',
                          opacity: isOpen ? 1 : 0.45,
                          transition: 'all 0.15s ease',
                          boxShadow: isSelected ? '0 0 10px rgba(16, 185, 129, 0.3)' : 'none'
                        }}
                      >
                        <div style={{ fontSize: '0.75rem', fontWeight: 700, color: isOpen ? 'var(--status-active)' : 'var(--text-dim)' }}>
                          {slot.batterySlotId || 'Bay'} • {isOpen ? 'AVAILABLE' : 'BOOKED'}
                        </div>
                        <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '4px' }}>
                          {slot.startTime?.substring(0, 5)} - {slot.endTime?.substring(0, 5)}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Confirm Button */}
            <button
              type="submit"
              disabled={submitting || !selectedSlot}
              className="btn btn-success"
              style={{
                width: '100%',
                padding: '14px',
                fontSize: '1rem'
              }}
            >
              {submitting ? 'Confirming with Smart Microgrid...' : '✓ Confirm Slot Reservation'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default SlotBookingPage;
