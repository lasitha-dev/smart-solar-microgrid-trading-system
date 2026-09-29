/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Reusable UI component for reservation status badges with curated premium styling.
 */

import React from 'react';

const StatusBadge = ({ status = 'PENDING' }) => {
  const normStatus = (status || 'PENDING').toUpperCase();

  const getStyle = () => {
    switch (normStatus) {
      case 'PENDING':
        return {
          background: '#FFF8E1',
          color: '#F57F17',
          border: '1px solid #FFE082',
          icon: '⏳'
        };
      case 'APPROVED':
        return {
          background: '#E3F2FD',
          color: '#1565C0',
          border: '1px solid #90CAF9',
          icon: '✓'
        };
      case 'COMPLETED':
        return {
          background: '#E8F5E9',
          color: '#2E7D32',
          border: '1px solid #A5D6A7',
          icon: '★'
        };
      case 'CANCELLED':
        return {
          background: '#FFEBEE',
          color: '#C62828',
          border: '1px solid #FFCDD2',
          icon: '✕'
        };
      default:
        return {
          background: '#F5F5F5',
          color: '#616161',
          border: '1px solid #E0E0E0',
          icon: '•'
        };
    }
  };

  const style = getStyle();

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: '4px 12px',
        borderRadius: '9999px',
        fontSize: '0.8rem',
        fontWeight: '700',
        letterSpacing: '0.04em',
        textTransform: 'uppercase',
        backgroundColor: style.background,
        color: style.color,
        border: style.border,
        boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
        transition: 'all 0.2s ease'
      }}
    >
      <span style={{ fontSize: '0.85em' }}>{style.icon}</span>
      {normStatus}
    </span>
  );
};

export default StatusBadge;
