import React from 'react';
import { Link } from 'react-router-dom';
import { ServiceRequest } from '../../types';
import StatusBadge from '../common/StatusBadge';
import { priorityLabel, statusMessage } from '../../utils/statusLabels';
import { formatAge, formatDateTime } from '../../utils/dateFormat';

interface RequestCardProps {
  request: ServiceRequest;
  to: string;
  audience?: 'requester' | 'staff';
}

export const REF_TAG_CLASS = 'inline-flex items-center px-3 py-0.5 border-2 border-ink border-l-8 border-l-signal rounded-sm bg-white text-ink font-display font-extrabold tracking-wide tabular-nums';

const RequestCard: React.FC<RequestCardProps> = ({ request, to, audience = 'requester' }) => {
  const isStaff = audience === 'staff';

  return (
    <li className='bg-white border border-line rounded-xl hover:border-ink'>
      <Link to={to} className='block px-5 py-4 rounded-xl text-inherit no-underline'>
        <div className='flex flex-wrap items-center gap-2.5'>
          <span className={REF_TAG_CLASS}>{request.referenceNumber}</span>
          <StatusBadge status={request.status} />
          {isStaff && request.overdue && <span className='px-2.5 py-0.5 rounded-sm bg-red-700 text-white text-[0.9375rem] font-bold'>Overdue</span>}
          {isStaff && request.priority === 'HIGH' && (
            <span className='px-2.5 py-0.5 rounded-sm border border-signal bg-amber-50 text-amber-900 text-[0.9375rem] font-bold'>{priorityLabel(request.priority)} priority</span>
          )}
        </div>

        <p className='my-3 line-clamp-2'>{request.description}</p>

        <dl className='flex flex-wrap gap-x-7 gap-y-2 m-0'>
          <div>
            <dt className='text-sm text-muted'>Category</dt>
            <dd className='m-0 font-bold'>{request.categoryName}</dd>
          </div>
          {isStaff ? (
            <>
              <div>
                <dt className='text-sm text-muted'>Age</dt>
                <dd className='m-0 font-bold'>{formatAge(request.createdAt)}</dd>
              </div>
              <div>
                <dt className='text-sm text-muted'>Owner</dt>
                <dd className='m-0 font-bold'>{request.assignedStaffName || 'Unassigned'}</dd>
              </div>
            </>
          ) : (
            <div>
              <dt className='text-sm text-muted'>Last updated</dt>
              <dd className='m-0 font-bold'>{formatDateTime(request.updatedAt)}</dd>
            </div>
          )}
        </dl>

        {!isStaff && <p className='mt-3 mb-0 pt-3 border-t border-line text-muted'>{statusMessage(request.status)}</p>}
      </Link>
    </li>
  );
};

export default RequestCard;
