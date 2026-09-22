/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Staff Profile Management for Backoffice Officers and Grid Operators with dynamic dirty tracking, read-only identity attributes, live password complexity validation, sleek form styling, and self-account deletion.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import {
  User,
  Shield,
  Lock,
  Mail,
  Phone,
  KeyRound,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Save,
  RotateCcw,
  Eye,
  EyeOff,
  Check,
  X,
  Radio,
  Trash2,
  AlertTriangle
} from 'lucide-react';

export const StaffProfilePage = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  // Profile data state
  const [profile, setProfile] = useState(null);
  const [originalProfile, setOriginalProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState('');

  // Editable fields state
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState('');
  const [saveErrorMsg, setSaveErrorMsg] = useState('');

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordErrorMsg, setPasswordErrorMsg] = useState('');
  const [passwordSuccessModal, setPasswordSuccessModal] = useState(false);

  // Account deletion state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteConfirmEmail, setDeleteConfirmEmail] = useState('');
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [deleteErrorMsg, setDeleteErrorMsg] = useState('');

  // Fetch initial profile
  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);
      setFetchError('');
      try {
        const data = await authService.getProfile();
        setProfile(data);
        setOriginalProfile(data);
        setFullName(data?.fullName || '');
        setPhoneNumber(data?.phone || data?.phoneNumber || '');
      } catch (err) {
        setFetchError(err.message || 'Failed to fetch user profile information.');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  // Deactivated status check
  const isDeactivated = profile?.status === 'Deactivated';

  // Dirty checking for Profile Details
  const isDirty = !isDeactivated && originalProfile !== null && (
    fullName.trim() !== (originalProfile.fullName || '').trim() ||
    phoneNumber.trim() !== (originalProfile.phone || originalProfile.phoneNumber || '').trim()
  );

  // Live Password Complexity Checks
  const hasMinLength = newPassword.length >= 6;
  const hasUpper = /[A-Z]/.test(newPassword);
  const hasLower = /[a-z]/.test(newPassword);
  const hasDigit = /[0-9]/.test(newPassword);
  const hasSymbol = /[^a-zA-Z0-9]/.test(newPassword);
  const isPasswordValid = hasMinLength && hasUpper && hasLower && hasDigit && hasSymbol;
  const passwordsMatch = newPassword.length > 0 && newPassword === confirmPassword;
  const canSubmitPassword = !isDeactivated && currentPassword.trim().length > 0 && isPasswordValid && passwordsMatch && !isChangingPassword;

  // Account Deletion Email Matching Check
  const registeredEmail = (profile?.email || '').trim();
  const isDeleteEmailMatch = deleteConfirmEmail.trim().toLowerCase() === registeredEmail.toLowerCase() && registeredEmail.length > 0;
  const canConfirmDelete = !isDeactivated && isDeleteEmailMatch && !isDeletingAccount;

  // Handle Save Profile Changes
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (isDeactivated || !isDirty) return;

    setIsSaving(true);
    setSaveErrorMsg('');
    setSaveSuccessMsg('');

    try {
      const updated = await authService.updateProfile({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim()
      });

      setProfile(updated);
      setOriginalProfile(updated);
      setFullName(updated.fullName || '');
      setPhoneNumber(updated.phone || updated.phoneNumber || '');
      setSaveSuccessMsg('Profile details successfully updated and synchronized!');
      setTimeout(() => setSaveSuccessMsg(''), 4000);
    } catch (err) {
      setSaveErrorMsg(err.message || 'Failed to update profile.');
    } finally {
      setIsSaving(false);
    }
  };

  // Reset editable fields to baseline
  const handleResetProfile = () => {
    if (originalProfile) {
      setFullName(originalProfile.fullName || '');
      setPhoneNumber(originalProfile.phone || originalProfile.phoneNumber || '');
      setSaveErrorMsg('');
      setSaveSuccessMsg('');
    }
  };

  // Handle Change Password Submit
  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (isDeactivated || !canSubmitPassword) return;

    setIsChangingPassword(true);
    setPasswordErrorMsg('');

    try {
      await authService.changePassword(
        currentPassword.trim(),
        newPassword.trim(),
        confirmPassword.trim()
      );

      // Open success modal and trigger auto-logout
      setPasswordSuccessModal(true);
    } catch (err) {
      setPasswordErrorMsg(err.message || 'Failed to change password. Please check your current password.');
      setIsChangingPassword(false);
    }
  };

  // Handle Account Deletion Submit
  const handleDeleteAccountSubmit = async (e) => {
    e.preventDefault();
    if (isDeactivated || !canConfirmDelete) return;

    setIsDeletingAccount(true);
    setDeleteErrorMsg('');

    try {
      await authService.deleteAccount(deleteConfirmEmail.trim());
      // Account deleted successfully: clear session and redirect
      logout();
      navigate('/login');
    } catch (err) {
      setDeleteErrorMsg(err.message || 'Failed to delete account. Please try again.');
      setIsDeletingAccount(false);
    }
  };

  // Handle Auto-Logout Confirmation after Password Change
  const handleProceedToLogin = () => {
    logout();
    navigate('/login');
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '6rem 2rem', color: 'var(--solar-amber)' }}>
        <span className="spinner" style={{ width: '40px', height: '40px', marginBottom: '1.25rem' }}></span>
        <p style={{ color: 'var(--text-muted)', fontSize: '1rem' }}>Retrieving administrative profile from server...</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1050px', margin: '0 auto', paddingBottom: '3rem' }}>
      
      {/* Top-Left Back Button Navigation Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', marginBottom: '1.5rem' }}>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => navigate(-1)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.55rem 1rem',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(17, 26, 46, 0.75)',
            backdropFilter: 'blur(10px)',
            borderColor: 'var(--border-light)',
            color: 'var(--text-main)',
            fontWeight: 600,
            fontSize: '0.875rem',
            boxShadow: '0 2px 6px rgba(0, 0, 0, 0.25)',
            transition: 'all 0.2s ease'
          }}
          title="Return to previous screen"
        >
          <ArrowLeft size={18} style={{ color: 'var(--solar-amber)' }} />
          <span>Back</span>
        </button>
      </div>

      {/* Page Header with Icon and Role Badge */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1.25rem',
        marginBottom: '2rem',
        paddingBottom: '1.25rem',
        borderBottom: '1px solid var(--border-subtle)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: 'var(--radius-md)',
            background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(217, 119, 6, 0.1))',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 15px rgba(245, 158, 11, 0.15)'
          }}>
            <User size={26} style={{ color: 'var(--solar-amber)' }} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0, letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
              My Staff Profile
            </h1>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: '0.25rem 0 0 0' }}>
              Manage your staff identity, update personal details, and configure security access credentials.
            </p>
          </div>
        </div>

        {/* Role Badge */}
        <div>
          <span
            className="badge"
            style={{
              background: profile?.role === 'GridOperator' ? 'var(--role-operator-bg, rgba(6, 182, 212, 0.15))' : 'var(--role-backoffice-bg, rgba(139, 92, 246, 0.15))',
              color: profile?.role === 'GridOperator' ? 'var(--role-operator, #06b6d4)' : 'var(--role-backoffice, #a78bfa)',
              borderColor: profile?.role === 'GridOperator' ? 'rgba(6, 182, 212, 0.4)' : 'rgba(139, 92, 246, 0.4)',
              padding: '0.55rem 1.15rem',
              fontSize: '0.85rem',
              fontWeight: 700,
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              borderRadius: 'var(--radius-full)',
              letterSpacing: '0.04em',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.3)'
            }}
          >
            {profile?.role === 'GridOperator' ? <Radio size={16} /> : <Shield size={16} />}
            <span>{profile?.role === 'GridOperator' ? 'GRID OPERATOR' : (profile?.role === 'Backoffice' ? 'BACKOFFICE OFFICER' : (profile?.role || 'STAFF OFFICER'))}</span>
          </span>
        </div>
      </div>

      {fetchError && (
        <div className="alert alert-danger" style={{ marginBottom: '1.75rem' }}>
          <AlertCircle size={18} />
          <span>{fetchError}</span>
        </div>
      )}

      {isDeactivated && (
        <div
          className="alert alert-danger"
          style={{
            marginBottom: '1.75rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.85rem',
            background: 'rgba(239, 68, 68, 0.12)',
            borderColor: 'rgba(239, 68, 68, 0.35)',
            color: '#fca5a5',
            padding: '1rem 1.25rem',
            borderRadius: 'var(--radius-md)'
          }}
        >
          <AlertCircle size={22} style={{ color: 'var(--status-danger, #ef4444)', flexShrink: 0 }} />
          <div>
            <strong style={{ display: 'block', color: '#fff', fontSize: '0.95rem', marginBottom: '0.15rem' }}>Account Deactivated</strong>
            <span style={{ fontSize: '0.85rem', color: '#fca5a5' }}>
              Your account is currently deactivated. Profile modifications, password updates, and account deletion are restricted.
            </span>
          </div>
        </div>
      )}

      {/* Main Grid Layout */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))', gap: '1.75rem' }}>
        
        {/* Left Column: Account Details & Editable Profile */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
          
          {/* Card 1: Read-Only System Identity */}
          <div className="card" style={{ padding: '1.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.85rem' }}>
              <Lock size={18} style={{ color: 'var(--solar-amber)' }} />
              <h3 style={{ fontSize: '1.1rem', margin: 0, fontWeight: 700 }}>System Identity & Credentials</h3>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
              <div>
                <label className="form-label" style={{ fontSize: '0.75rem', color: 'var(--text-dim)', letterSpacing: '0.04em' }}>
                  NATIONAL IDENTITY CARD (NIC)
                </label>
                <div style={{
                  background: '#0b1120',
                  border: '1px solid var(--border-light)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontFamily: 'monospace',
                  fontSize: '0.95rem',
                  fontWeight: 600,
                  color: 'var(--text-main)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  boxShadow: 'inset 0 1px 3px rgba(0, 0, 0, 0.4)'
                }}>
                  <span>{profile?.nic || 'N/A'}</span>
                  <Lock size={14} style={{ color: 'var(--text-dim)' }} />
                </div>
              </div>

              <div>
                <label className="form-label" style={{ fontSize: '0.75rem', color: 'var(--text-dim)', letterSpacing: '0.04em' }}>
                  SYSTEM USERNAME
                </label>
                <div style={{
                  background: '#0b1120',
                  border: '1px solid var(--border-light)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontFamily: 'monospace',
                  fontSize: '0.95rem',
                  fontWeight: 600,
                  color: 'var(--text-main)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  boxShadow: 'inset 0 1px 3px rgba(0, 0, 0, 0.4)'
                }}>
                  <span>@{profile?.username || 'N/A'}</span>
                  <Lock size={14} style={{ color: 'var(--text-dim)' }} />
                </div>
              </div>

              <div style={{ gridColumn: 'span 2' }}>
                <label className="form-label" style={{ fontSize: '0.75rem', color: 'var(--text-dim)', letterSpacing: '0.04em' }}>
                  REGISTERED OFFICIAL EMAIL (IMMUTABLE)
                </label>
                <div style={{
                  background: '#0b1120',
                  border: '1px solid var(--border-light)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.95rem',
                  color: 'var(--text-main)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  boxShadow: 'inset 0 1px 3px rgba(0, 0, 0, 0.4)'
                }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                    <Mail size={16} style={{ color: 'var(--solar-amber)' }} />
                    <span style={{ fontWeight: 500 }}>{profile?.email || 'N/A'}</span>
                  </span>
                  <Lock size={14} style={{ color: 'var(--text-dim)' }} title="Permanent system identifier" />
                </div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '0.4rem', lineHeight: 1.4 }}>
                  * Email address is permanently bound to this account for authentication and security notifications.
                </p>
              </div>
            </div>
          </div>

          {/* Card 2: Personal Contact Information (Editable with Dirty Tracking) */}
          <div className="card" style={{ padding: '1.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.85rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                <User size={18} style={{ color: 'var(--solar-amber)' }} />
                <h3 style={{ fontSize: '1.1rem', margin: 0, fontWeight: 700 }}>Personal & Contact Details</h3>
              </div>
              {isDirty && (
                <span className="badge badge-pending" style={{ fontSize: '0.725rem', padding: '0.25rem 0.65rem' }}>
                  Unsaved Changes
                </span>
              )}
            </div>

            {saveSuccessMsg && (
              <div className="alert alert-success" style={{ marginBottom: '1.25rem', padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                <CheckCircle2 size={18} />
                <span>{saveSuccessMsg}</span>
              </div>
            )}

            {saveErrorMsg && (
              <div className="alert alert-danger" style={{ marginBottom: '1.25rem', padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                <AlertCircle size={18} />
                <span>{saveErrorMsg}</span>
              </div>
            )}

            <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="staffFullName">
                  Full Name
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="staffFullName"
                    type="text"
                    className="form-control"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Enter full legal name"
                    style={{ paddingLeft: '2.6rem' }}
                    disabled={isDeactivated || isSaving}
                    required
                  />
                  <User
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="staffPhone">
                  Phone Number
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="staffPhone"
                    type="tel"
                    className="form-control"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    placeholder="e.g. 0771234567"
                    style={{ paddingLeft: '2.6rem' }}
                    disabled={isDeactivated || isSaving}
                  />
                  <Phone
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                {isDirty && (
                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={handleResetProfile}
                    disabled={isDeactivated || isSaving}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.55rem 0.95rem' }}
                  >
                    <RotateCcw size={15} />
                    <span>Cancel</span>
                  </button>
                )}

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isDeactivated || !isDirty || isSaving}
                  style={{
                    opacity: (isDeactivated || !isDirty) ? 0.5 : 1,
                    cursor: (isDeactivated || !isDirty) ? 'not-allowed' : 'pointer',
                    padding: '0.6rem 1.25rem'
                  }}
                >
                  {isSaving ? (
                    <>
                      <span className="spinner"></span>
                      <span>Saving Changes...</span>
                    </>
                  ) : (
                    <>
                      <Save size={16} />
                      <span>Save Changes</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right Column: Security & Password Management */}
        <div>
          <div className="card" style={{ padding: '1.75rem', height: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.85rem' }}>
              <KeyRound size={18} style={{ color: 'var(--solar-amber)' }} />
              <h3 style={{ fontSize: '1.1rem', margin: 0, fontWeight: 700 }}>Security & Password Management</h3>
            </div>

            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1.25rem', lineHeight: 1.45 }}>
              To ensure microgrid system integrity, updating your security credentials will immediately invalidate your active session and require re-authentication.
            </p>

            {passwordErrorMsg && (
              <div className="alert alert-danger" style={{ marginBottom: '1.25rem', padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                <AlertCircle size={18} />
                <span>{passwordErrorMsg}</span>
              </div>
            )}

            <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              
              {/* Current Password */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="currentPassword">
                  Current Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="currentPassword"
                    type={showCurrentPassword ? 'text' : 'password'}
                    className="form-control"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter current password"
                    style={{ paddingLeft: '2.6rem', paddingRight: '2.6rem' }}
                    disabled={isDeactivated || isChangingPassword}
                    required
                  />
                  <Lock
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    disabled={isDeactivated}
                    style={{
                      position: 'absolute',
                      right: '0.85rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-dim)',
                      cursor: isDeactivated ? 'not-allowed' : 'pointer',
                      padding: '0.2rem',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                    title={showCurrentPassword ? "Hide password" : "Show password"}
                  >
                    {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* New Password */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="newPassword">
                  New Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="newPassword"
                    type={showNewPassword ? 'text' : 'password'}
                    className="form-control"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter new strong password"
                    style={{ paddingLeft: '2.6rem', paddingRight: '2.6rem' }}
                    disabled={isDeactivated || isChangingPassword}
                    required
                  />
                  <KeyRound
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    disabled={isDeactivated}
                    style={{
                      position: 'absolute',
                      right: '0.85rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-dim)',
                      cursor: isDeactivated ? 'not-allowed' : 'pointer',
                      padding: '0.2rem',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                    title={showNewPassword ? "Hide password" : "Show password"}
                  >
                    {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* Live Complexity Chips */}
              <div style={{
                background: 'rgba(15, 23, 42, 0.65)',
                padding: '0.85rem 1rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-light)'
              }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', marginBottom: '0.5rem', letterSpacing: '0.04em' }}>
                  PASSWORD COMPLEXITY REQUIREMENTS:
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.45rem' }}>
                  <span style={{
                    fontSize: '0.725rem',
                    padding: '0.25rem 0.55rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    background: hasMinLength ? 'rgba(34, 197, 94, 0.15)' : 'rgba(148, 163, 184, 0.1)',
                    color: hasMinLength ? 'var(--status-active)' : 'var(--text-dim)',
                    border: `1px solid ${hasMinLength ? 'rgba(34, 197, 94, 0.3)' : 'transparent'}`
                  }}>
                    {hasMinLength ? <Check size={13} /> : <X size={13} />} 6+ Characters
                  </span>

                  <span style={{
                    fontSize: '0.725rem',
                    padding: '0.25rem 0.55rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    background: hasUpper ? 'rgba(34, 197, 94, 0.15)' : 'rgba(148, 163, 184, 0.1)',
                    color: hasUpper ? 'var(--status-active)' : 'var(--text-dim)',
                    border: `1px solid ${hasUpper ? 'rgba(34, 197, 94, 0.3)' : 'transparent'}`
                  }}>
                    {hasUpper ? <Check size={13} /> : <X size={13} />} Uppercase (A-Z)
                  </span>

                  <span style={{
                    fontSize: '0.725rem',
                    padding: '0.25rem 0.55rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    background: hasLower ? 'rgba(34, 197, 94, 0.15)' : 'rgba(148, 163, 184, 0.1)',
                    color: hasLower ? 'var(--status-active)' : 'var(--text-dim)',
                    border: `1px solid ${hasLower ? 'rgba(34, 197, 94, 0.3)' : 'transparent'}`
                  }}>
                    {hasLower ? <Check size={13} /> : <X size={13} />} Lowercase (a-z)
                  </span>

                  <span style={{
                    fontSize: '0.725rem',
                    padding: '0.25rem 0.55rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    background: hasDigit ? 'rgba(34, 197, 94, 0.15)' : 'rgba(148, 163, 184, 0.1)',
                    color: hasDigit ? 'var(--status-active)' : 'var(--text-dim)',
                    border: `1px solid ${hasDigit ? 'rgba(34, 197, 94, 0.3)' : 'transparent'}`
                  }}>
                    {hasDigit ? <Check size={13} /> : <X size={13} />} Number (0-9)
                  </span>

                  <span style={{
                    fontSize: '0.725rem',
                    padding: '0.25rem 0.55rem',
                    borderRadius: 'var(--radius-sm)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.3rem',
                    background: hasSymbol ? 'rgba(34, 197, 94, 0.15)' : 'rgba(148, 163, 184, 0.1)',
                    color: hasSymbol ? 'var(--status-active)' : 'var(--text-dim)',
                    border: `1px solid ${hasSymbol ? 'rgba(34, 197, 94, 0.3)' : 'transparent'}`
                  }}>
                    {hasSymbol ? <Check size={13} /> : <X size={13} />} Symbol (!@#$)
                  </span>
                </div>
              </div>

              {/* Confirm Password */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="confirmPassword">
                  Confirm New Password
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="confirmPassword"
                    type={showConfirmPassword ? 'text' : 'password'}
                    className="form-control"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Repeat new password"
                    style={{ paddingLeft: '2.6rem', paddingRight: '2.6rem' }}
                    disabled={isDeactivated || isChangingPassword}
                    required
                  />
                  <Lock
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    disabled={isDeactivated}
                    style={{
                      position: 'absolute',
                      right: '0.85rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-dim)',
                      cursor: isDeactivated ? 'not-allowed' : 'pointer',
                      padding: '0.2rem',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                    title={showConfirmPassword ? "Hide password" : "Show password"}
                  >
                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>

                {/* Match indicator */}
                {confirmPassword.length > 0 && (
                  <div style={{
                    marginTop: '0.45rem',
                    fontSize: '0.75rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    fontWeight: 600,
                    color: passwordsMatch ? 'var(--status-active)' : 'var(--status-pending)'
                  }}>
                    {passwordsMatch ? <Check size={15} /> : <X size={15} />}
                    <span>{passwordsMatch ? 'Passwords match' : 'Passwords do not match'}</span>
                  </div>
                )}
              </div>

              <div style={{ marginTop: '0.5rem' }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{
                    width: '100%',
                    justifyContent: 'center',
                    opacity: (!canSubmitPassword || isDeactivated) ? 0.5 : 1,
                    cursor: (!canSubmitPassword || isDeactivated) ? 'not-allowed' : 'pointer',
                    padding: '0.65rem'
                  }}
                  disabled={!canSubmitPassword || isDeactivated}
                >
                  {isChangingPassword ? (
                    <>
                      <span className="spinner"></span>
                      <span>Updating Credentials...</span>
                    </>
                  ) : (
                    <>
                      <KeyRound size={16} />
                      <span>Update Password & Re-Authenticate</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      {/* Danger Zone: Account Deletion Card */}
      <div className="card" style={{
        marginTop: '1.75rem',
        padding: '1.75rem',
        border: '1px solid rgba(239, 68, 68, 0.35)',
        background: 'linear-gradient(180deg, rgba(239, 68, 68, 0.05) 0%, rgba(15, 23, 42, 0.6) 100%)',
        boxShadow: '0 4px 20px rgba(239, 68, 68, 0.08)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1.25rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
              <Trash2 size={20} style={{ color: 'var(--status-danger, #ef4444)' }} />
              <h3 style={{ fontSize: '1.1rem', margin: 0, fontWeight: 700, color: 'var(--status-danger, #ef4444)' }}>
                Danger Zone: Account Deletion
              </h3>
            </div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', margin: 0, maxWidth: '650px', lineHeight: 1.45 }}>
              Permanently delete your account and remove all personal information, credentials, and access logs from the microgrid network.
            </p>
          </div>

          <button
            type="button"
            className="btn btn-danger"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.6rem 1.25rem',
              opacity: isDeactivated ? 0.5 : 1,
              cursor: isDeactivated ? 'not-allowed' : 'pointer'
            }}
            disabled={isDeactivated}
            onClick={() => {
              if (isDeactivated) return;
              setDeleteConfirmEmail('');
              setDeleteErrorMsg('');
              setIsDeleteModalOpen(true);
            }}
          >
            <Trash2 size={16} />
            <span>Delete My Account</span>
          </button>
        </div>
      </div>

      {/* Account Deletion Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="modal-overlay" style={{ zIndex: 110 }}>
          <div className="modal-content" style={{ maxWidth: '500px', padding: '2rem' }}>
            <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                background: 'rgba(239, 68, 68, 0.15)',
                color: 'var(--status-danger, #ef4444)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '1rem',
                boxShadow: '0 0 20px rgba(239, 68, 68, 0.2)'
              }}>
                <AlertTriangle size={34} />
              </div>
              <h2 style={{ fontSize: '1.4rem', marginBottom: '0.4rem', fontWeight: 700, color: 'var(--status-danger, #ef4444)' }}>
                Delete Account Permanently?
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', lineHeight: 1.45, margin: 0 }}>
                This action <strong style={{ color: '#fff' }}>cannot be undone</strong>. Your administrative access to the Smart Solar Microgrid Trading System will be immediately and permanently erased.
              </p>
            </div>

            {deleteErrorMsg && (
              <div className="alert alert-danger" style={{ marginBottom: '1.25rem', padding: '0.75rem 1rem', fontSize: '0.875rem' }}>
                <AlertCircle size={18} />
                <span>{deleteErrorMsg}</span>
              </div>
            )}

            <form onSubmit={handleDeleteAccountSubmit}>
              <div style={{
                background: '#0b1120',
                padding: '0.9rem 1rem',
                borderRadius: 'var(--radius-md)',
                marginBottom: '1.25rem',
                border: '1px solid var(--border-light)'
              }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
                  YOUR REGISTERED EMAIL ADDRESS:
                </div>
                <div style={{ fontWeight: 700, color: 'var(--solar-amber)', fontSize: '0.95rem', wordBreak: 'break-all' }}>
                  {profile?.email || 'N/A'}
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                <label className="form-label" htmlFor="confirmEmailInput" style={{ fontSize: '0.85rem' }}>
                  To confirm, please type your email address below:
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="confirmEmailInput"
                    type="email"
                    className="form-control"
                    value={deleteConfirmEmail}
                    onChange={(e) => setDeleteConfirmEmail(e.target.value)}
                    placeholder={profile?.email || 'Enter registered email'}
                    style={{ paddingLeft: '2.6rem' }}
                    autoComplete="off"
                    required
                  />
                  <Mail
                    size={18}
                    style={{
                      position: 'absolute',
                      left: '0.9rem',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      color: 'var(--text-dim)',
                      pointerEvents: 'none'
                    }}
                  />
                </div>

                {/* Email Match Status */}
                {deleteConfirmEmail.length > 0 && (
                  <div style={{
                    marginTop: '0.5rem',
                    fontSize: '0.8rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    fontWeight: 600,
                    color: isDeleteEmailMatch ? 'var(--status-active)' : 'var(--status-danger, #ef4444)'
                  }}>
                    {isDeleteEmailMatch ? <Check size={16} /> : <X size={16} />}
                    <span>{isDeleteEmailMatch ? 'Email confirmed. You may now permanently delete your account.' : 'Email does not match registered address.'}</span>
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.85rem' }}>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setIsDeleteModalOpen(false)}
                  disabled={isDeletingAccount}
                  style={{ padding: '0.6rem 1.15rem' }}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="btn btn-danger"
                  disabled={!canConfirmDelete}
                  style={{
                    opacity: !canConfirmDelete ? 0.5 : 1,
                    cursor: !canConfirmDelete ? 'not-allowed' : 'pointer',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.45rem',
                    padding: '0.6rem 1.25rem'
                  }}
                >
                  {isDeletingAccount ? (
                    <>
                      <span className="spinner"></span>
                      <span>Deleting Account...</span>
                    </>
                  ) : (
                    <>
                      <Trash2 size={16} />
                      <span>Permanently Delete Account</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Auto-Logout Success Modal */}
      {passwordSuccessModal && (
        <div className="modal-overlay" style={{ zIndex: 100 }}>
          <div className="modal-content" style={{ maxWidth: '460px', textAlign: 'center', padding: '2.25rem' }}>
            <div style={{
              width: '68px',
              height: '68px',
              borderRadius: '50%',
              background: 'var(--status-active-bg)',
              color: 'var(--status-active)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '1.25rem',
              boxShadow: '0 0 25px rgba(16, 185, 129, 0.25)'
            }}>
              <CheckCircle2 size={38} />
            </div>

            <h2 style={{ fontSize: '1.45rem', marginBottom: '0.5rem', fontWeight: 700 }}>Password Changed!</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: '1.75rem' }}>
              Your account password has been successfully updated across all microgrid services. For security, your current session has been terminated. Please log in with your new credentials.
            </p>

            <button
              type="button"
              className="btn btn-primary"
              style={{ width: '100%', justifyContent: 'center', padding: '0.75rem' }}
              onClick={handleProceedToLogin}
            >
              <span>Proceed to Login</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
