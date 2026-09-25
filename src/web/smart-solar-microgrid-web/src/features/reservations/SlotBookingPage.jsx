/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Prosumer-facing Web Booking Page with 7-day calendar restriction and slot booking.
 */

import React, { useState, useEffect } from 'react';
import { reservationService } from '../../services/reservationService';

const STATIONS = [
  { id: '60d5ec49f1b2c42d8c3b4a59', name: 'Station A – Solar Bay (Main Microgrid)' },
  { id: '60d5ec49f1b2c42d8c3b4a60', name: 'Station B – North Grid (Substation)' }
];

const SlotBookingPage = () => {
  const todayStr = new Date().toISOString().split('T')[0];
  const maxDate = new Date();
  maxDate.setDate(maxDate.getDate() + 7);
  const maxDateStr = maxDate.toISOString().split('T')[0];

  const [stationId, setStationId] = useState(STATIONS[0].id);
  const [selectedDate, setSelectedDate] = useState(todayStr);
  const [prosumerNic, setProsumerNic] = useState('200112345678');
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);
  const [successBooking, setSuccessBooking] = useState(null);

  useEffect(() => {
    fetchSlots();
  }, [stationId, selectedDate]);

  const fetchSlots = async () => {
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
    <div style={{ minHeight: '100vh', backgroundColor: '#F8FAFC', padding: '36px 20px', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ marginBottom: '28px', textAlign: 'center' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', background: '#DCFCE7', padding: '6px 14px', borderRadius: '9999px', color: '#166534', fontWeight: 700, fontSize: '0.8rem', marginBottom: '8px' }}>
            <span>⚡</span> PROSUMER ENERGY RESERVATION
          </div>
          <h1 style={{ margin: 0, fontSize: '1.8rem', fontWeight: 800, color: '#0F172A' }}>
            Reserve Solar Microgrid Charging Slot
          </h1>
          <p style={{ margin: '8px 0 0', color: '#64748B', fontSize: '0.95rem' }}>
            Secure clean energy slots in advance up to 7 days ahead.
          </p>
        </div>

        {/* Success Modal / Banner */}
        {successBooking && (
          <div
            style={{
              backgroundColor: '#ECFDF5',
              border: '2px solid #10B981',
              borderRadius: '16px',
              padding: '24px',
              marginBottom: '28px',
              boxShadow: '0 4px 6px -1px rgba(16, 185, 129, 0.1)',
              textAlign: 'center'
            }}
          >
            <span style={{ fontSize: '3rem' }}>🎉</span>
            <h2 style={{ margin: '8px 0', color: '#065F46', fontSize: '1.4rem', fontWeight: 800 }}>
              Reservation Successfully Confirmed!
            </h2>
            <p style={{ color: '#047857', margin: '4px 0 16px' }}>
              Your energy slot has been reserved. Reference ID:
            </p>
            <div style={{ display: 'inline-block', backgroundColor: '#FFFFFF', padding: '10px 20px', borderRadius: '8px', border: '1px dashed #10B981', fontFamily: 'monospace', fontWeight: 800, fontSize: '1.1rem', color: '#065F46' }}>
              {successBooking.id}
            </div>
            <p style={{ marginTop: '16px', fontSize: '0.85rem', color: '#047857' }}>
              Status: <strong>Pending Operator Approval</strong>. You can view this booking anytime in your mobile app.
            </p>
            <button
              onClick={() => {
                setSuccessBooking(null);
                fetchSlots();
              }}
              style={{
                marginTop: '12px',
                padding: '10px 22px',
                backgroundColor: '#059669',
                color: '#FFF',
                border: 'none',
                borderRadius: '8px',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              Book Another Slot
            </button>
          </div>
        )}

        {/* Error Alert */}
        {errorBanner && (
          <div
            style={{
              backgroundColor: '#FEF2F2',
              border: '1px solid #FECACA',
              color: '#991B1B',
              padding: '14px 18px',
              borderRadius: '10px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              fontWeight: 600
            }}
          >
            <span>⚠️</span>
            <div>{errorBanner}</div>
          </div>
        )}

        {/* Booking Form Card */}
        <div
          style={{
            background: '#FFFFFF',
            borderRadius: '16px',
            padding: '28px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)'
          }}
        >
          <form onSubmit={handleBook}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px', marginBottom: '24px' }}>
              {/* Station Selection */}
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Select Microgrid Station
                </label>
                <select
                  value={stationId}
                  onChange={(e) => setStationId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #CBD5E1',
                    fontSize: '0.9rem',
                    backgroundColor: '#FFF'
                  }}
                >
                  {STATIONS.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Date Selection with 7-day rule */}
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                  Target Date (7-Day Limit)
                </label>
                <input
                  type="date"
                  value={selectedDate}
                  min={todayStr}
                  max={maxDateStr}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '9px 14px',
                    borderRadius: '8px',
                    border: '1px solid #CBD5E1',
                    fontSize: '0.9rem'
                  }}
                />
              </div>
            </div>

            {/* Prosumer NIC */}
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '6px' }}>
                Prosumer National Identity Card (NIC)
              </label>
              <input
                type="text"
                placeholder="e.g. 200112345678 or 981234567V"
                value={prosumerNic}
                onChange={(e) => setProsumerNic(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #CBD5E1',
                  fontSize: '0.9rem'
                }}
              />
            </div>

            {/* Slots Grid */}
            <div style={{ marginBottom: '24px' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '8px' }}>
                Available Battery Slots for {selectedDate}
              </label>

              {loadingSlots ? (
                <div style={{ padding: '24px', textAlign: 'center', color: '#64748B' }}>
                  Loading energy slots...
                </div>
              ) : slots.length === 0 ? (
                <div style={{ padding: '24px', textAlign: 'center', color: '#94A3B8', border: '1px dashed #CBD5E1', borderRadius: '8px' }}>
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
                            ? '2px solid #16A34A'
                            : isOpen
                            ? '1px solid #CBD5E1'
                            : '1px solid #E2E8F0',
                          backgroundColor: isSelected
                            ? '#F0FDF4'
                            : isOpen
                            ? '#FFFFFF'
                            : '#F1F5F9',
                          cursor: isOpen ? 'pointer' : 'not-allowed',
                          opacity: isOpen ? 1 : 0.5,
                          transition: 'all 0.15s ease',
                          boxShadow: isSelected ? '0 0 0 2px rgba(22, 163, 74, 0.2)' : 'none'
                        }}
                      >
                        <div style={{ fontSize: '0.75rem', fontWeight: 700, color: isOpen ? '#16A34A' : '#94A3B8' }}>
                          {slot.batterySlotId || 'Bay'} • {isOpen ? 'AVAILABLE' : 'BOOKED'}
                        </div>
                        <div style={{ fontSize: '1rem', fontWeight: 700, color: '#1E293B', marginTop: '4px' }}>
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
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '10px',
                border: 'none',
                backgroundColor: selectedSlot ? '#16A34A' : '#CBD5E1',
                color: '#FFFFFF',
                fontSize: '1rem',
                fontWeight: 700,
                cursor: selectedSlot ? 'pointer' : 'not-allowed',
                boxShadow: selectedSlot ? '0 4px 6px -1px rgba(22, 163, 74, 0.3)' : 'none',
                transition: 'all 0.2s ease'
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
