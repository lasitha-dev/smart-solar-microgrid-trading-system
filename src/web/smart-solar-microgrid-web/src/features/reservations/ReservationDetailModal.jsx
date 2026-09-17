/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: React component displaying full details of a reservation.
 */

import React from 'react';
import StatusBadge from './components/StatusBadge';

const ReservationDetailModal = ({ reservation, onClose }) => {
  if (!reservation) return null;

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog">
        <div className="modal-content">
          
          <div className="modal-header">
            <h5 className="modal-title">Reservation Details</h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          
          <div className="modal-body">
            <p><strong>ID:</strong> {reservation.id}</p>
            <p><strong>Prosumer NIC:</strong> {reservation.prosumerId}</p>
            <p><strong>Station ID:</strong> {reservation.stationId}</p>
            <p><strong>Slot ID:</strong> {reservation.bookingSlotId}</p>
            <p><strong>Scheduled For:</strong> {new Date(reservation.scheduledDateTime).toLocaleString()}</p>
            <p><strong>Status:</strong> <StatusBadge status={reservation.status} /></p>
            
            {reservation.qrCode && (
              <div className="alert alert-info mt-3">
                <strong>QR Payload:</strong><br />
                <small className="text-break">{reservation.qrCode}</small>
              </div>
            )}
            
            {reservation.cancelReason && (
              <p className="text-danger mt-2"><strong>Cancel Reason:</strong> {reservation.cancelReason}</p>
            )}
          </div>
          
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Close</button>
          </div>
          
        </div>
      </div>
    </div>
  );
};

export default ReservationDetailModal;
