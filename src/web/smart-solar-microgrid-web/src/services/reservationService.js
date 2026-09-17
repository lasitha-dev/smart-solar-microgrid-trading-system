/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Axios service layer for reservations API.
 */

import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'https://localhost:7198/api';

const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    headers: {
      Authorization: `Bearer ${token}`
    }
  };
};

export const reservationService = {
  getProsumerReservations: async (prosumerId) => {
    const response = await axios.get(`${API_BASE_URL}/reservations?prosumerId=${prosumerId}`, getAuthHeaders());
    return response.data;
  },

  getAllReservations: async () => {
    // Uses the same endpoint but without prosumerId filter for the Operator view
    const response = await axios.get(`${API_BASE_URL}/reservations`, getAuthHeaders());
    return response.data;
  },

  approveReservation: async (id, operatorId) => {
    const response = await axios.patch(`${API_BASE_URL}/reservations/${id}/approve?operatorId=${operatorId}`, {}, getAuthHeaders());
    return response.data;
  }
};
