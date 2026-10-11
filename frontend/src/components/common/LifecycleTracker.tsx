import React from 'react';
import { HistoryEntry, Status } from '../../types';
import { STATUS_ORDER, statusLabel } from '../../utils/statusLabels';
import { formatDateTime } from '../../utils/dateFormat';

interface LifecycleTrackerProps {
  status: Status;
  history?: HistoryEntry[];
  dark?: boolean;
}

const LifecycleTracker: React.FC<LifecycleTrackerProps> = ({ status, history = [], dark = false }) => {
  const currentIndex = STATUS_ORDER.indexOf(status);
  if (currentIndex < 0) return null;

  const line = dark ? 'bg-slate-500' : 'bg-slate-300';
  const done = dark ? 'bg-white' : 'bg-ink';
  const reachedAt = (stage: Status) => history.find((entry) => entry.newStatus === stage)?.changedAt;

  return (
    <ol className={`flex flex-col md:flex-row m-0 p-0 list-none ${dark ? 'text-white' : ''}`} aria-label="Request progress">
      {STATUS_ORDER.map((stage, index) => {
        const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'upcoming';
        const at = reachedAt(stage);
        const lit = state !== 'upcoming';

        const marker =
          state === 'done'
            ? `${dark ? 'bg-white border-white text-ink' : 'bg-ink border-ink text-white'}`
            : state === 'current'
              ? `bg-signal ring-4 ring-signal/35 ${dark ? 'border-white' : 'border-ink'}`
              : `${dark ? 'bg-ink border-slate-500' : 'bg-white border-slate-300'}`;

        return (
          <li key={stage} className='relative flex md:flex-1 md:flex-col items-start md:items-center gap-3.5 md:gap-2.5 pb-5 md:pb-0 last:pb-0 md:px-1 text-left md:text-center' aria-current={state === 'current' ? 'step' : undefined}>
            {/* Connector */}
            {index > 0 && <span className={`hidden md:block absolute top-3 right-1/2 w-full h-1 ${index <= currentIndex ? done : line}`} aria-hidden="true" />}
            {index < STATUS_ORDER.length - 1 && <span className={`md:hidden absolute left-3 top-7 bottom-0 w-1 ${index < currentIndex ? done : line}`} aria-hidden="true" />}

            <span className={`relative z-10 grid place-items-center shrink-0 w-7 h-7 rounded-full border-[3px] ${marker}`} aria-hidden="true">
              {state === 'done' && (
                <svg width="14" height="14" viewBox="0 0 16 16" focusable="false">
                  <path d="M3.5 8.5l3 3 6-6.5" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </span>

            <span className='flex flex-col leading-snug pt-0.5 md:pt-0'>
              <span className={lit ? 'font-bold' : `font-normal ${dark ? 'text-slate-400' : 'text-muted'}`}>{statusLabel(stage)}</span>
              {state === 'done' && <span className='sr-only'> (completed)</span>}
              {state === 'upcoming' && <span className='sr-only'> (not reached yet)</span>}
              {at && lit && <span className={`text-[0.8125rem] ${dark ? 'text-slate-400' : 'text-muted'}`}>{formatDateTime(at)}</span>}
            </span>
          </li>
        );
      })}
    </ol>
  );
};

export default LifecycleTracker;
