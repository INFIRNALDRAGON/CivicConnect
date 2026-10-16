import React, { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useRequestDetail } from '../../hooks/useRequests';
import { api, ApiError, Body } from '../../utils/api';
import { ServiceRequest } from '../../types';
import { formatDateTime } from '../../utils/dateFormat';
import { TRANSITIONS, TRANSITION_ACTIONS, priorityLabel, statusLabel } from '../../utils/statusLabels';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Field from '../../components/common/Field';
import LifecycleTracker from '../../components/common/LifecycleTracker';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Panel from '../../components/common/Panel';
import StatusBadge from '../../components/common/StatusBadge';
import StatusHistoryTimeline from '../../components/requests/StatusHistoryTimeline';
import { REF_TAG_CLASS } from '../../components/requests/RequestCard';

const MIN_RESOLUTION_NOTE = 10;

interface Feedback {
  type: 'success' | 'error' | 'conflict';
  text: string;
}

interface RequestActionsProps {
  request: ServiceRequest;
  onChanged: () => void;
}

const FACT_LABEL = 'text-sm text-muted';
const FACT_VALUE = 'm-0 font-bold';

const RequestActions: React.FC<RequestActionsProps> = ({ request, onChanged }) => {
  const next = request.allowedTransitions ?? TRANSITIONS[request.status];
  const canAccept = next.includes('ASSIGNED');
  const canReject = next.includes('REJECTED');
  const nextStatus = next.find((status) => status !== 'ASSIGNED' && status !== 'REJECTED');
  const readOnly = request.status === 'RESOLVED' || request.status === 'CLOSED' || request.status === 'REJECTED';
  const [statusNote, setStatusNote] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [commentText, setCommentText] = useState('');
  const [busy, setBusy] = useState('');
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ status?: string; comment?: string; reject?: string }>({});

  const run = async (kind: string, path: string, body: Body, successText: string, clear?: () => void) => {
    setBusy(kind);
    setFeedback(null);
    try {
      await api.post(`/requests/${request.id}/${path}`, { ...body, version: request.version });
      if (clear) clear();
      setFeedback({ type: 'success', text: successText });
      await onChanged();
    } catch (error) {
      const failure = error as ApiError;
      setFeedback({ type: failure.status === 409 ? 'conflict' : 'error', text: failure.message });
    } finally {
      setBusy('');
    }
  };

  const submitStatus = (event: React.FormEvent) => {
    event.preventDefault();
    if (!nextStatus) return;
    const note = statusNote.trim();
    if (nextStatus === 'RESOLVED' && note.length < MIN_RESOLUTION_NOTE) {
      setFieldErrors({ status: `Write a resolution note of at least ${MIN_RESOLUTION_NOTE} characters so the requester knows what was done.` });
      return;
    }
    setFieldErrors({});
    run('status', 'status', { targetStatus: nextStatus, comment: note }, `Status changed to ${statusLabel(nextStatus)}.`, () => setStatusNote(''));
  };

  const submitReject = (event: React.FormEvent) => {
    event.preventDefault();
    const reason = rejectReason.trim();
    if (reason.length < MIN_RESOLUTION_NOTE) {
      setFieldErrors({ reject: `Give a reason of at least ${MIN_RESOLUTION_NOTE} characters so the requester understands why.` });
      return;
    }
    setFieldErrors({});
    run('reject', 'status', { targetStatus: 'REJECTED', comment: reason }, 'The request was rejected and the requester has been told why.', () => setRejectReason(''));
  };

  const submitComment = (event: React.FormEvent) => {
    event.preventDefault();
    const text = commentText.trim();
    if (!text) {
      setFieldErrors({ comment: 'Write a note before adding it.' });
      return;
    }
    setFieldErrors({});
    run('comment', 'comments', { comment: text }, 'Note added.', () => setCommentText(''));
  };

  const noteLabel = nextStatus === 'RESOLVED' ? 'Resolution note' : nextStatus === 'CLOSED' ? 'Closing note' : 'Note for the history';

  return (
    <div className='grid gap-4'>
      <h2 className='text-xl'>Actions</h2>

      {/* Feedback */}
      {feedback?.type === 'success' && <Alert variant="success">{feedback.text}</Alert>}
      {feedback?.type === 'error' && (
        <Alert variant="error" title="That did not work">
          {feedback.text}
        </Alert>
      )}
      {feedback?.type === 'conflict' && (
        <Alert
          variant="warning"
          title="This request changed"
          action={
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                setFeedback(null);
                onChanged();
              }}
            >
              Load latest
            </Button>
          }
        >
          {feedback.text}
        </Alert>
      )}

      {/* Accept */}
      {canAccept && (
        <div className='grid gap-3 pt-4 border-t border-line'>
          <p className='m-0'>Nobody owns this request yet. Accepting it records you as the owner and moves it to Assigned.</p>
          <Button size="lg" loading={busy === 'accept'} disabled={Boolean(busy)} onClick={() => run('accept', 'assign', {}, 'You are now responsible for this request.')}>
            Accept responsibility
          </Button>
        </div>
      )}

      {/* Next status */}
      {nextStatus && (
        <form className='grid gap-3 pt-4 border-t border-line' onSubmit={submitStatus} noValidate>
          <Field id="status-note" label={noteLabel} optional={nextStatus === 'IN_PROGRESS'} hint="The requester will see this note." error={fieldErrors.status}>
            <textarea rows={4} value={statusNote} onChange={(event) => setStatusNote(event.target.value)} />
          </Field>
          <Button type="submit" loading={busy === 'status'} disabled={Boolean(busy)}>
            {TRANSITION_ACTIONS[nextStatus]}
          </Button>
        </form>
      )}

      {/* Reject */}
      {canReject && (
        <form className='grid gap-3 pt-4 border-t border-line' onSubmit={submitReject} noValidate>
          <Field id="reject-reason" label="Reject this request" hint="Use this when the request cannot be accepted. The requester will see your reason." error={fieldErrors.reject}>
            <textarea rows={3} value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} />
          </Field>
          <Button type="submit" variant="secondary" loading={busy === 'reject'} disabled={Boolean(busy)}>
            {TRANSITION_ACTIONS.REJECTED}
          </Button>
        </form>
      )}

      {/* Notes */}
      {!readOnly && (
        <form className='grid gap-3 pt-4 border-t border-line' onSubmit={submitComment} noValidate>
          <Field id="comment-text" label="Add a note" hint="Notes are stored with your name and the time." error={fieldErrors.comment}>
            <textarea rows={3} value={commentText} onChange={(event) => setCommentText(event.target.value)} />
          </Field>
          <Button type="submit" variant="secondary" loading={busy === 'comment'} disabled={Boolean(busy)}>
            Add note
          </Button>
        </form>
      )}

      {readOnly && (
        <Alert variant="info" title={request.status === 'CLOSED' ? 'This request is closed' : request.status === 'REJECTED' ? 'This request was rejected' : 'This request is read-only'}>
          {request.status === 'CLOSED'
            ? 'Closed requests are final and cannot be changed.'
            : request.status === 'REJECTED'
              ? 'Rejected requests are final. The reason is in the history.'
              : 'It has been resolved, so notes and edits are no longer accepted. You can close it.'}
        </Alert>
      )}
    </div>
  );
};

const RequestDetailPage: React.FC = () => {
  const { id = '' } = useParams();
  const { data: request, loading, error, reload, refresh } = useRequestDetail(id);

  if (loading && !request) return <LoadingSpinner label="Loading request" />;

  if (error || !request) {
    return (
      <div className='max-w-3xl'>
        <Link className='inline-block mb-4 font-bold' to="/staff/queue">
          Back to the queue
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
          {error?.message}
        </Alert>
      </div>
    );
  }

  return (
    <div className='max-w-6xl'>
      <Link className='inline-block mb-4 font-bold' to="/staff/queue">
        Back to the queue
      </Link>

      <header className='flex flex-wrap items-center gap-3'>
        <span className={`${REF_TAG_CLASS} px-4 py-1.5 text-2xl`}>{request.referenceNumber}</span>
        <h1 className='sr-only'>Request {request.referenceNumber}</h1>
        <StatusBadge status={request.status} />
        {request.overdue && <span className='px-2.5 py-0.5 rounded-sm bg-red-700 text-white text-[0.9375rem] font-bold'>Overdue</span>}
        {request.priority === 'HIGH' && <span className='px-2.5 py-0.5 rounded-sm border border-signal bg-amber-50 text-amber-900 text-[0.9375rem] font-bold'>High priority</span>}
      </header>

      <div className='grid lg:grid-cols-[minmax(0,1fr)_360px] gap-x-6 items-start'>
        {/* Main column */}
        <div className='min-w-0'>
          <Panel labelledBy="progress-heading">
            <h2 id="progress-heading" className='mb-3 text-xl'>
              Progress
            </h2>
            <LifecycleTracker status={request.status} history={request.history} />
          </Panel>

          <Panel labelledBy="detail-heading">
            <h2 id="detail-heading" className='mb-3 text-xl'>
              Request detail
            </h2>
            <p className='m-0 whitespace-pre-wrap break-words'>{request.description}</p>
            <dl className='grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-x-6 gap-y-3.5 mt-4 mb-0'>
              <div>
                <dt className={FACT_LABEL}>Category</dt>
                <dd className={FACT_VALUE}>{request.categoryName}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Priority</dt>
                <dd className={FACT_VALUE}>{priorityLabel(request.priority)}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Requester reference</dt>
                <dd className={FACT_VALUE}>{request.requesterReference}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Owner</dt>
                <dd className={FACT_VALUE}>{request.assignedStaffName || 'Unassigned'}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Accepted</dt>
                <dd className={FACT_VALUE}>{formatDateTime(request.assignedAt)}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Submitted</dt>
                <dd className={FACT_VALUE}>{formatDateTime(request.createdAt)}</dd>
              </div>
              <div>
                <dt className={FACT_LABEL}>Last updated</dt>
                <dd className={FACT_VALUE}>{formatDateTime(request.updatedAt)}</dd>
              </div>
              {request.resolvedAt && (
                <div>
                  <dt className={FACT_LABEL}>Resolved</dt>
                  <dd className={FACT_VALUE}>{formatDateTime(request.resolvedAt)}</dd>
                </div>
              )}
            </dl>
          </Panel>

          <Panel labelledBy="history-heading">
            <h2 id="history-heading" className='mb-3 text-xl'>
              History and notes
            </h2>
            <p className='text-muted'>Times are shown in South African time (SAST).</p>
            <StatusHistoryTimeline entries={request.history} showActor />
          </Panel>
        </div>

        {/* Actions column */}
        <Panel as="aside" label="Request actions" className='order-first lg:order-none lg:sticky lg:top-20'>
          <RequestActions request={request} onChanged={refresh} />
        </Panel>
      </div>
    </div>
  );
};

export default RequestDetailPage;
