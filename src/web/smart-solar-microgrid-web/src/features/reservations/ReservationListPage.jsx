/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: React page for Grid Operators to view and approve reservations.
 */

import React, { useState, useEffect } from 'react';
import { reservationService } from '../../services/reservationService';
import StatusBadge from './components/StatusBadge';
import ReservationDetailModal from './ReservationDetailModal';

const ReservationListPage = () => {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedRes, setSelectedRes] = useState(null);

  // Assuming operator ID is available from an auth context
  const operatorId = "OP-12345"; 

  useEffect(() => {
    fetchReservations();
  }, []);

  const fetchReservations = async () => {
    try {
      setLoading(true);
      const res = await reservationService.getAllReservations();
      if (res.success) {
        setReservations(res.data);
      } else {
        setError(res.message);
      }
    } catch (err) {
      setError("Failed to fetch reservations.");
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      const res = await reservationService.approveReservation(id, operatorId);
      if (res.success) {
        alert("Reservation approved successfully!");
        fetchReservations(); // Refresh list
      } else {
        alert(`Error: ${res.message}`);
      }
    } catch (err) {
      alert("Failed to approve reservation.");
    }
  };

  if (loading) return <div>Loading reservations...</div>;
  if (error) return <div className="alert alert-danger">{error}</div>;

  return (
    <div className="container mt-4">
      <h2>Grid Operator - Reservation Management</h2>
      <table className="table table-striped mt-3">
        <thead>
          <tr>
            <th>ID</th>
            <th>Prosumer</th>
            <th>Station</th>
            <th>Scheduled Time</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {reservations.map(res => (
            <tr key={res.id}>
              <td>{res.id.substring(0, 8)}...</td>
              <td>{res.prosumerId}</td>
              <td>{res.stationId}</td>
              <td>{new Date(res.scheduledDateTime).toLocaleString()}</td>
              <td><StatusBadge status={res.status} /></td>
              <td>
                <button 
                  className="btn btn-sm btn-info me-2"
                  onClick={() => setSelectedRes(res)}
                >
                  View
                </button>
                {res.status === 'Pending' && (
                  <button 
                    className="btn btn-sm btn-success"
                    onClick={() => handleApprove(res.id)}
                  >
                    Approve
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedRes && (
        <ReservationDetailModal 
          reservation={selectedRes} 
          onClose={() => setSelectedRes(null)} 
        />
      )}
    </div>
  );
};

export default ReservationListPage;
