/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Authentication service for Backoffice officers and Administrators.
 */

import api from './api';

export const authService = {
  /**
   * Authenticates administrative user credentials.
   * @param {string} identifier - Username or NIC.
   * @param {string} password - User password.
   * @returns {Promise<object>} Auth payload including token, user details, and role.
   */
  login: async (identifier, password) => {
    const response = await api.post('/auth/login', {
      identifier: identifier.trim(),
      password,
    });
    
    // API returns ApiResponseDto<LoginResponseDto> -> { success, message, data }
    const authData = response.data || response;
    
    if (authData.token) {
      localStorage.setItem('ssmts_token', authData.token);
      localStorage.setItem('ssmts_user', JSON.stringify({
        userId: authData.userId,
        nic: authData.nic,
        username: authData.username,
        role: authData.role,
      }));
    }
    
    return authData;
  },

  /**
   * Logs out the user and clears stored credentials.
   */
  logout: () => {
    localStorage.removeItem('ssmts_token');
    localStorage.removeItem('ssmts_user');
  },

  /**
   * Retrieves current logged in user from local storage.
   */
  getCurrentUser: () => {
    try {
      const userStr = localStorage.getItem('ssmts_user');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  /**
   * Checks if user has a valid stored token.
   */
  isAuthenticated: () => {
    return !!localStorage.getItem('ssmts_token');
  },

  /**
   * Fetches the current logged in user's profile from the API.
   * @returns {Promise<object>} User profile details.
   */
  getProfile: async () => {
    const response = await api.get('/auth/profile');
    return response.data || response;
  },

  /**
   * Updates current user's profile details (fullName, phoneNumber).
   * @param {object} profileData - { fullName, phoneNumber }
   * @returns {Promise<object>} Updated user profile.
   */
  updateProfile: async (profileData) => {
    const response = await api.put('/auth/profile', profileData);
    const updated = response.data || response;
    // Update cached user info in local storage if needed
    const currentUser = authService.getCurrentUser();
    if (currentUser && updated) {
      localStorage.setItem('ssmts_user', JSON.stringify({
        ...currentUser,
        fullName: updated.fullName || currentUser.fullName,
      }));
    }
    return updated;
  },

  /**
   * Changes current user's password and clears session.
   * @param {string} currentPassword
   * @param {string} newPassword
   * @param {string} confirmNewPassword
   * @returns {Promise<object>} Server response.
   */
  changePassword: async (currentPassword, newPassword, confirmNewPassword) => {
    const response = await api.post('/auth/change-password', {
      currentPassword,
      newPassword,
      confirmNewPassword,
    });
    return response.data || response;
  },

  /**
   * Permanently deletes the current user's account after confirming with their registered email address.
   * @param {string} confirmEmail
   * @returns {Promise<object>} Server response.
   */
  deleteAccount: async (confirmEmail) => {
    const response = await api.post('/auth/delete-account', {
      confirmEmail: confirmEmail.trim(),
    });
    return response.data || response;
  }
};
