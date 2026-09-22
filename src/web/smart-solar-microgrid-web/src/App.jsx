/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Master React router tree wiring protected Backoffice administration routes.
 */

import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { AdminLayout } from './layouts/AdminLayout';
import { LoginPage } from './pages/LoginPage';
import { PendingApprovalsPage } from './pages/PendingApprovalsPage';
import { UserManagementPage } from './pages/UserManagementPage';
import { StaffProfilePage } from './pages/StaffProfilePage';
import { NotFoundPage } from './pages/NotFoundPage';

export const App = () => {
  return (
    <Routes>
      {/* Public Authentication Route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Protected Backoffice Administrative Routes */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/pending-approvals" replace />} />
        <Route path="pending-approvals" element={<PendingApprovalsPage />} />
        <Route path="users" element={<UserManagementPage />} />
        <Route path="profile" element={<StaffProfilePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
};
