import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useRequestDetail } from '../../hooks/useRequests';
import { formatAgo, formatDateTime } from '../../utils/dateFormat';
import { priorityLabel, statusMessage } from '../../utils/statusLabels';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import LifecycleTracker from '../../components/common/LifecycleTracker';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Panel from '../../components/common/Panel';
import StatusBadge from '../../components/common/StatusBadge';
import StatusHistoryTimeline from '../../components/requests/StatusHistoryTimeline';
import { REF_TAG_CLASS } from '../../components/requests/RequestCard';

const RequestDetailPage: React.FC = () => {
  const { id = '' } = useParams();
  const { data: request, loading, error, reload } = useRequestDetail(id, 30000);

  if (loading && !request) return <LoadingSpinner label="Loading your request" />;

  if (error || !request) {
    return (
      <div className='max-w-3xl'>
        <Link className='inline-block mb-4 font-bold' to="/requests">
          Back to my requests
        </Link>
        <Alert
          variant="error"
          title="This request could not be shown"
          action={
            error && (error.status >= 500 || error.status === 0) ? (
              <Button variant="secondary" size="sm" onClick={reload}>
                Try again
              </Button>
            ) : null
          }
        >
          {error?.status === 404 ? 'We could not find a request with that reference among yours.' : error?.message}
        </Alert>
      </div>
    );
  }

  return (
    <div className='max-w-3xl'>
      <Link className='inline-block mb-4 font-bold' to="/requests">
        Back to my requests
      </Link>

      <header className='flex flex-wrap items-center gap-3'>
        <span className={`${REF_TAG_CLASS} px-4 py-1.5 text-2xl`}>{request.referenceNumber}</span>
        <StatusBadge status={request.status} />
      </header>

      {/* Current status */}
      <section className='mt-5 p-6 bg-white border border-line border-t-[6px] border-t-signal rounded-xl' aria-live="polite" aria-labelledby="status-heading">
        <h1 id="status-heading" className='max-w-[34ch] text-2xl'>
          {statusMessage(request.status)}
        </h1>
        <p className='mt-2 mb-7 text-[0.9375rem] text-muted'>
          Last updated {formatAgo(request.updatedAt)} on {formatDateTime(request.updatedAt)}. This page checks for updates every 30 seconds.
        </p>
        <LifecycleTracker status={request.status} history={request.history} />
      </section>

      {/* Request details */}
      <Panel labelledBy="details-heading">
        <h2 id="details-heading" className='mb-3 text-xl'>
          Your request
        </h2>
        <dl className='grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-x-6 gap-y-3.5 mt-4 mb-4'>
          <div>
            <dt className='text-sm text-muted'>Category</dt>
            <dd className='m-0 font-bold'>{request.categoryName}</dd>
          </div>
          <div>
            <dt className='text-sm text-muted'>Urgency</dt>
            <dd className='m-0 font-bold'>{priorityLabel(request.priority)}</dd>
          </div>
          <div>
            <dt className='text-sm text-muted'>Submitted</dt>
            <dd className='m-0 font-bold'>{formatDateTime(request.createdAt)}</dd>
          </div>
          <div>
            <dt className='text-sm text-muted'>Assigned to staff</dt>
            <dd className='m-0 font-bold'>{request.assigned ? 'Yes' : 'Not yet'}</dd>
          </div>
        </dl>
        <p className='m-0 whitespace-pre-wrap break-words'>{request.description}</p>
      </Panel>

      {/* History */}
      <Panel labelledBy="history-heading">
        <h2 id="history-heading" className='mb-3 text-xl'>
          History
        </h2>
        <p className='text-muted'>Times are shown in South African time (SAST).</p>
        <StatusHistoryTimeline entries={request.history} />
      </Panel>
    </div>
  );
};

export default RequestDetailPage;
