/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: API service integration for User Management, Search/Filtering, and Prosumer Approval Workflow.
 */

import api from './api';

export const adminService = {
  /**
   * Retrieves users matching optional search query, role filter, status filter, and pagination.
   */
  getUsers: async ({ search = '', role = '', status = '', page = 1, pageSize = 50 } = {}) => {
    const params = new URLSearchParams();
    if (search && search.trim()) params.append('search', search.trim());
    if (role && role !== 'ALL') params.append('role', role);
    if (status && status !== 'ALL') params.append('status', status);
    params.append('page', page);
    params.append('pageSize', pageSize);

    const response = await api.get(`/admin/users?${params.toString()}`);
    return response.data || [];
  },

  /**
   * Retrieves all prosumers currently awaiting Backoffice review/activation.
   */
  getPendingProsumers: async () => {
    const response = await api.get('/admin/prosumers/pending');
    return response.data || [];
  },

  /**
   * Transitions a user's lifecycle state (Active, Deactivated, PendingActivation) with optional reason.
   */
  updateUserStatus: async (userId, status, reason = null) => {
    const response = await api.patch(`/admin/users/${userId}/status`, {
      status,
      reason,
    });
    return response.data;
  },

  /**
   * Activates/approves a pending prosumer or reactivates an account.
   */
  activateUser: async (userId) => {
    const response = await api.patch(`/admin/users/${userId}/activate`);
    return response.data;
  },

  /**
   * Deactivates a user account.
   */
  deactivateUser: async (userId) => {
    const response = await api.patch(`/admin/users/${userId}/deactivate`);
    return response.data;
  },

  /**
   * Creates a new administrative Backoffice or Grid Operator staff user.
   */
  createStaffUser: async (staffData) => {
    const response = await api.post('/admin/users', staffData);
    return response.data;
  }
};
