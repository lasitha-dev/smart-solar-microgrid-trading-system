/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Reusable UI component for reservation status badges (React).
 */

import React from 'react';

const StatusBadge = ({ status }) => {
  let badgeClass = "badge rounded-pill ";
  
  switch (status.toUpperCase()) {
    case 'PENDING':
      badgeClass += "bg-warning text-dark";
      break;
    case 'APPROVED':
      badgeClass += "bg-primary";
      break;
    case 'COMPLETED':
      badgeClass += "bg-success";
      break;
    case 'CANCELLED':
      badgeClass += "bg-danger";
      break;
    default:
      badgeClass += "bg-secondary";
  }

  return (
    <span className={badgeClass}>
      {status.toUpperCase()}
    </span>
  );
};

export default StatusBadge;
