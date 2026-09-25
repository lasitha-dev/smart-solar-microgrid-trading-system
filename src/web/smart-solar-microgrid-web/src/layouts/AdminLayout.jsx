/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Master layout for Backoffice Web Admin Portal with sidebar navigation and status indicators.
 */

import React, { useState, useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { adminService } from '../services/adminService';
import {
  Sun,
  Users,
  User,
  UserCheck,
  LogOut,
  Shield,
  Activity,
  Menu,
  X,
  Zap,
  Radio,
  Calendar,
  Clock
} from 'lucide-react';

export const AdminLayout = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);

  // Fetch pending count periodically for backoffice users
  const fetchPendingCount = async () => {
    if (user?.role !== 'Backoffice' && user?.role !== 'Administrator') return;
    try {
      const pending = await adminService.getPendingProsumers();
      setPendingCount(pending?.length || 0);
    } catch {
      // Ignored if offline or unauthorized
    }
  };

  useEffect(() => {
    if (user?.role === 'Backoffice' || user?.role === 'Administrator') {
      fetchPendingCount();
      const interval = setInterval(fetchPendingCount, 15000); // refresh every 15s
      return () => clearInterval(interval);
    }
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-container">
      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div
          className="modal-overlay"
          style={{ zIndex: 35 }}
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar Navigation */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        {/* Brand Header */}
        <div style={{
          padding: '1.5rem',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem'
        }}>
          <div style={{
            background: 'linear-gradient(135deg, #f59e0b, #d97706)',
            color: '#0b1120',
            padding: '0.6rem',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: 'var(--shadow-glow)'
          }}>
            <Sun size={24} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.1rem', color: 'var(--text-main)', lineHeight: 1.2 }}>Smart Solar</h2>
            <p style={{ fontSize: '0.75rem', color: 'var(--solar-amber)', fontWeight: 600, letterSpacing: '0.05em' }}>
              {user?.role === 'GridOperator' ? 'OPERATIONS PORTAL' : 'BACKOFFICE PORTAL'}
            </p>
          </div>
        </div>

        {/* Navigation Menu Links */}
        <nav style={{ padding: '1.25rem 0.75rem', flex: 1 }}>
          {(user?.role === 'Backoffice' || user?.role === 'Administrator') && (
            <>
              <div style={{ fontSize: '0.7rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 700, padding: '0 0.75rem 0.5rem', letterSpacing: '0.08em' }}>
                Account Administration
              </div>

              <NavLink
                to="/pending-approvals"
                onClick={() => setSidebarOpen(false)}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
                  background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
                  borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
                  fontWeight: isActive ? 600 : 500,
                  fontSize: '0.9rem',
                  marginBottom: '0.4rem',
                  transition: 'all var(--transition-fast)'
                })}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <UserCheck size={18} style={{ color: 'var(--solar-amber)' }} />
                  <span>Pending Approvals</span>
                </div>
                {pendingCount > 0 && (
                  <span className="badge badge-pending" style={{ fontSize: '0.7rem', padding: '0.15rem 0.5rem' }}>
                    {pendingCount}
                  </span>
                )}
              </NavLink>

              <NavLink
                to="/users"
                onClick={() => setSidebarOpen(false)}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
                  background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
                  borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
                  fontWeight: isActive ? 600 : 500,
                  fontSize: '0.9rem',
                  marginBottom: '0.4rem',
                  transition: 'all var(--transition-fast)'
                })}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <Users size={18} style={{ color: 'var(--role-operator)' }} />
                  <span>User Directory</span>
                </div>
              </NavLink>

              <NavLink
                to="/admin/stations"
                onClick={() => setSidebarOpen(false)}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
                  background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
                  borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
                  fontWeight: isActive ? 600 : 500,
                  fontSize: '0.9rem',
                  marginBottom: '0.4rem',
                  transition: 'all var(--transition-fast)'
                })}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <Zap size={18} style={{ color: 'var(--solar-amber)' }} />
                  <span>Microgrid Stations</span>
                </div>
              </NavLink>
            </>
          )}

          <div style={{ fontSize: '0.7rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 700, padding: '1rem 0.75rem 0.5rem', letterSpacing: '0.08em' }}>
            Energy & Reservations
          </div>

          <NavLink
            to="/reservations"
            onClick={() => setSidebarOpen(false)}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
              background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
              borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
              fontWeight: isActive ? 600 : 500,
              fontSize: '0.9rem',
              marginBottom: '0.4rem',
              transition: 'all var(--transition-fast)'
            })}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Calendar size={18} style={{ color: 'var(--status-active)' }} />
              <span>Energy Reservations</span>
            </div>
          </NavLink>

          <NavLink
            to="/book-slot"
            onClick={() => setSidebarOpen(false)}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
              background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
              borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
              fontWeight: isActive ? 600 : 500,
              fontSize: '0.9rem',
              marginBottom: '0.4rem',
              transition: 'all var(--transition-fast)'
            })}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Clock size={18} style={{ color: 'var(--solar-amber)' }} />
              <span>Book Energy Slot</span>
            </div>
          </NavLink>

          <div style={{ fontSize: '0.7rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 700, padding: (user?.role === 'Backoffice' || user?.role === 'Administrator') ? '1rem 0.75rem 0.5rem' : '0 0.75rem 0.5rem', letterSpacing: '0.08em' }}>
            Account Settings
          </div>

          <NavLink
            to="/profile"
            onClick={() => setSidebarOpen(false)}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              color: isActive ? 'var(--text-main)' : 'var(--text-muted)',
              background: isActive ? 'var(--bg-surface-hover)' : 'transparent',
              borderLeft: isActive ? '3px solid var(--solar-amber)' : '3px solid transparent',
              fontWeight: isActive ? 600 : 500,
              fontSize: '0.9rem',
              marginBottom: '0.4rem',
              transition: 'all var(--transition-fast)'
            })}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <User size={18} style={{ color: 'var(--solar-amber)' }} />
              <span>My Profile</span>
            </div>
          </NavLink>
        </nav>

        {/* User Info & Logout Card */}
        <div style={{
          padding: '1rem',
          borderTop: '1px solid var(--border-subtle)',
          background: 'var(--bg-card-header)'
        }}>
          <div
            onClick={() => {
              setSidebarOpen(false);
              navigate('/profile');
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              marginBottom: '0.75rem',
              cursor: 'pointer',
              padding: '0.35rem',
              borderRadius: 'var(--radius-sm)',
              transition: 'background var(--transition-fast)'
            }}
            title="Click to view Staff Profile"
          >
            <div style={{
              background: user?.role === 'GridOperator' ? 'var(--role-operator-bg, rgba(6, 182, 212, 0.15))' : 'var(--role-backoffice-bg)',
              color: user?.role === 'GridOperator' ? 'var(--role-operator, #06b6d4)' : 'var(--role-backoffice)',
              padding: '0.5rem',
              borderRadius: 'var(--radius-full)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              {user?.role === 'GridOperator' ? <Radio size={18} /> : <Shield size={18} />}
            </div>
            <div style={{ overflow: 'hidden', flex: 1 }}>
              <p style={{ fontSize: '0.85rem', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.username || 'Staff'}
              </p>
              <p style={{ fontSize: '0.75rem', color: user?.role === 'GridOperator' ? 'var(--role-operator, #06b6d4)' : 'var(--text-muted)' }}>
                {user?.role === 'GridOperator' ? 'Grid Operator' : (user?.role === 'Backoffice' ? 'Backoffice Officer' : (user?.role || 'Staff'))}
              </p>
            </div>
          </div>

          <button
            className="btn btn-outline btn-sm"
            onClick={handleLogout}
            style={{ width: '100%', display: 'flex', justifyContent: 'center' }}
          >
            <LogOut size={16} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Page Wrapper */}
      <div className="main-content" style={{ minWidth: 0, overflowX: 'hidden' }}>
        {/* Top Navbar */}
        <header className="top-navbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button
              className="btn btn-outline btn-sm mobile-menu-toggle"
              onClick={() => setSidebarOpen(!sidebarOpen)}
              aria-label="Toggle navigation menu"
            >
              {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Zap size={18} style={{ color: 'var(--solar-amber)' }} />
              <span style={{ fontWeight: 600, fontSize: '0.95rem' }}>Smart Solar</span>
            </div>
          </div>
        </header>

        {/* Dynamic Nested Content */}
        <main className="page-body" style={{ minWidth: 0 }}>
          <Outlet context={{ refreshPendingCount: fetchPendingCount }} />
        </main>
      </div>
    </div>
  );
};
