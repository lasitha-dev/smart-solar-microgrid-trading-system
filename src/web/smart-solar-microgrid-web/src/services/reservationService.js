/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Axios service layer for reservations API with interceptor and all Member 3 endpoints.
 */

import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor to attach JWT auth token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token') || 'mock_token_operator_001';
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to extract structured error payloads
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const customError = {
      message: error.response?.data?.message || error.message || 'Unknown network error',
      code: error.response?.data?.code || (error.response ? `HTTP_${error.response.status}` : 'NETWORK_ERROR'),
      status: error.response?.status
    };
    return Promise.reject(customError);
  }
);

export const reservationService = {
  getProsumerReservations: async (prosumerId, status = null) => {
    const query = new URLSearchParams();
    if (prosumerId) query.append('prosumerId', prosumerId);
    if (status) query.append('status', status);
    const response = await api.get(`/reservations?${query.toString()}`);
    return response.data;
  },

  getAllReservations: async (status = null) => {
    const query = status ? `?status=${status}` : '';
    const response = await api.get(`/reservations${query}`);
    return response.data;
  },

  getReservationById: async (id) => {
    const response = await api.get(`/reservations/${id}`);
    return response.data;
  },

  createReservation: async (dto) => {
    const response = await api.post('/reservations', dto);
    return response.data;
  },

  updateReservation: async (id, dto) => {
    const response = await api.put(`/reservations/${id}`, dto);
    return response.data;
  },

  cancelReservation: async (id, reason = null) => {
    const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    const response = await api.delete(`/reservations/${id}${query}`);
    return response.data;
  },

  approveReservation: async (id, operatorId = 'OP-001') => {
    const response = await api.patch(`/reservations/${id}/approve?operatorId=${operatorId}`);
    return response.data;
  },

  rejectReservation: async (id, reason = null, operatorId = 'OP-001') => {
    const query = new URLSearchParams();
    if (reason) query.append('reason', reason);
    if (operatorId) query.append('operatorId', operatorId);
    const queryString = query.toString() ? `?${query.toString()}` : '';
    const response = await api.patch(`/reservations/${id}/reject${queryString}`);
    return response.data;
  },

  getAvailableSlots: async (stationId, date) => {
    const dateStr = date instanceof Date ? date.toISOString().split('T')[0] : date;
    const response = await api.get(`/reservations/slots?stationId=${stationId}&date=${dateStr}`);
    return response.data;
  },

  seedSlots: async () => {
    const response = await api.post('/reservations/seed');
    return response.data;
  }
};
