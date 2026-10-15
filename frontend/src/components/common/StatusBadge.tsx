import React from 'react';
import { Status } from '../../types';
import { statusLabel } from '../../utils/statusLabels';

const COLORS: Record<Status, string> = {
  SUBMITTED: 'bg-slate-200 text-slate-700',
  ASSIGNED: 'bg-purple-100 text-purple-900',
  IN_PROGRESS: 'bg-blue-100 text-blue-900',
  RESOLVED: 'bg-green-100 text-green-900',
  CLOSED: 'bg-gray-200 text-gray-700',
  REJECTED: 'bg-red-100 text-red-900'
};

const GLYPHS: Record<Status, React.ReactNode> = {
  SUBMITTED: <circle cx="8" cy="8" r="4.5" fill="none" stroke="currentColor" strokeWidth="2" />,
  ASSIGNED: (
    <>
      <circle cx="8" cy="8" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="8" cy="8" r="2" fill="currentColor" />
    </>
  ),
  IN_PROGRESS: (
    <>
      <circle cx="8" cy="8" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M8 2.5a5.5 5.5 0 0 1 0 11z" fill="currentColor" />
    </>
  ),
  RESOLVED: <path d="M3.5 8.5l3 3 6-6.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />,
  CLOSED: <rect x="3.5" y="3.5" width="9" height="9" rx="1.5" fill="currentColor" />,
  REJECTED: <path d="M4 4l8 8M12 4l-8 8" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
};

const StatusBadge: React.FC<{ status: Status }> = ({ status }) => {
  return (
    <span className={`inline-flex items-center gap-1.5 py-0.5 pl-2 pr-2.5 rounded-full text-[0.9375rem] font-bold whitespace-nowrap ${COLORS[status]}`}>
      <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
        {GLYPHS[status]}
      </svg>
      {statusLabel(status)}
    </span>
  );
};

export default StatusBadge;
