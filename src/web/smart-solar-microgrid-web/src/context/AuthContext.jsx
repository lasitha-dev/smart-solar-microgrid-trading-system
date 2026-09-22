/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: React Context managing global authentication state, JWT tokens, and Backoffice role gating.
 */

import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check initial authentication state from localStorage
    const savedToken = localStorage.getItem('ssmts_token');
    const savedUser = authService.getCurrentUser();

    if (savedToken && savedUser) {
      // Ensure user has administrative privileges
      if (savedUser.role === 'Backoffice' || savedUser.role === 'Administrator') {
        setToken(savedToken);
        setUser(savedUser);
      } else {
        // Clear non-administrative session
        authService.logout();
      }
    }
    setLoading(false);
  }, []);

  const login = async (identifier, password) => {
    const authData = await authService.login(identifier, password);
    
    // Strict role validation: Only Backoffice and Administrator allowed in web portal
    if (authData.role !== 'Backoffice' && authData.role !== 'Administrator') {
      authService.logout();
      throw new Error('Access Denied: The Backoffice Web Portal is restricted to Backoffice Officers and System Administrators only.');
    }

    const userData = {
      userId: authData.userId,
      nic: authData.nic,
      username: authData.username,
      role: authData.role,
    };

    setToken(authData.token);
    setUser(userData);
    return userData;
  };

  const logout = () => {
    authService.logout();
    setToken(null);
    setUser(null);
  };

  const isBackoffice = user?.role === 'Backoffice' || user?.role === 'Administrator';

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, isBackoffice }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
