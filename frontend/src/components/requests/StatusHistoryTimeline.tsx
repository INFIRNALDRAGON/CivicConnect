import React from 'react';
import { HistoryEntry } from '../../types';
import { statusLabel } from '../../utils/statusLabels';
import { formatDateTime } from '../../utils/dateFormat';

interface StatusHistoryTimelineProps {
  entries?: HistoryEntry[];
  showActor?: boolean;
}

const titleFor = (entry: HistoryEntry, showActor: boolean): string => {
  if (entry.previousStatus === entry.newStatus) return 'Note added';
  if (!entry.previousStatus) return 'Request submitted';
  if (showActor) return `Moved from ${statusLabel(entry.previousStatus)} to ${statusLabel(entry.newStatus)}`;
  return `Status changed to ${statusLabel(entry.newStatus)}`;
};

const StatusHistoryTimeline: React.FC<StatusHistoryTimelineProps> = ({ entries, showActor = false }) => {
  if (!entries || entries.length === 0) return <p className='text-muted'>No activity recorded yet.</p>;

  return (
    <ol className='mt-4 ml-2 p-0 list-none border-l-[3px] border-line'>
      {entries.map((entry) => {
        const isNote = entry.previousStatus === entry.newStatus;
        return (
          <li key={entry.id} className='relative pl-6 pb-5'>
            <span className={`absolute -left-[9px] top-1.5 w-3.5 h-3.5 rounded-full border-[3px] ${isNote ? 'border-line-strong bg-line' : 'border-ink bg-white'}`} aria-hidden="true" />
            <div className='flex flex-wrap justify-between gap-x-4 gap-y-1'>
              <span className='font-bold'>{titleFor(entry, showActor)}</span>
              <time className='text-[0.9375rem] text-muted' dateTime={entry.changedAt}>
                {formatDateTime(entry.changedAt)}
              </time>
            </div>
            {showActor && entry.changedByName && <p className='m-0 text-[0.9375rem] text-muted'>By {entry.changedByName}</p>}
            {entry.comment && <p className='mt-1.5 mb-0 px-3.5 py-2.5 rounded-md bg-paper whitespace-pre-wrap break-words'>{entry.comment}</p>}
          </li>
        );
      })}
    </ol>
  );
};

export default StatusHistoryTimeline;
