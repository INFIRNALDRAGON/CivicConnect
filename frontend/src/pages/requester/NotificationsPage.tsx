import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useNotifications } from '../../hooks/useRequests';
import { api, ApiError } from '../../utils/api';
import { formatDateTime } from '../../utils/dateFormat';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const NotificationsPage: React.FC = () => {
  const { data: notifications, loading, error, reload, refresh } = useNotifications(30000);
  const [busyId, setBusyId] = useState('');
  const [failure, setFailure] = useState('');

  const markRead = async (id: string) => {
    setBusyId(id);
    setFailure('');
    try {
      await api.post(`/notifications/${id}/read`);
      await refresh();
    } catch (caught) {
      setFailure((caught as ApiError).message);
    } finally {
      setBusyId('');
    }
  };

  const unread = notifications ? notifications.filter((item) => !item.readAt).length : 0;

  return (
    <div className='max-w-3xl'>
      <h1 className='text-3xl md:text-4xl'>Notifications</h1>
      <p className='mt-2 max-w-[62ch] text-muted'>Updates about your requests, newest first. {unread > 0 ? `${unread} unread.` : ''}</p>

      {loading && !notifications && <LoadingSpinner label="Loading your notifications" />}
      {error && (
        <Alert
          variant="error"
          title="Your notifications did not load"
          action={
            <Button variant="secondary" size="sm" onClick={reload}>
              Try again
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}
      {failure && (
        <Alert variant="error" title="That did not work">
          {failure}
        </Alert>
      )}
      {notifications && notifications.length === 0 && (
        <EmptyState title="No notifications yet">You will see a message here every time the status of one of your requests changes.</EmptyState>
      )}
      {notifications && notifications.length > 0 && (
        <ul className='grid gap-3 mt-4 p-0 list-none' aria-label="Your notifications">
          {notifications.map((item) => (
            <li key={item.id} className={`p-4 rounded-xl border-2 ${item.readAt ? 'border-line bg-white' : 'border-signal bg-amber-50'}`}>
              <div className='flex flex-wrap justify-between gap-x-4 gap-y-1'>
                <Link className='font-bold' to={`/requests/${item.requestId}`}>
                  {item.referenceNumber}
                </Link>
                <time className='text-[0.9375rem] text-muted' dateTime={item.createdAt}>
                  {formatDateTime(item.createdAt)}
                </time>
              </div>
              <p className='mt-1.5 mb-0'>{item.message}</p>
              {!item.readAt && (
                <div className='mt-3'>
                  <Button variant="secondary" size="sm" loading={busyId === item.id} disabled={Boolean(busyId)} onClick={() => markRead(item.id)}>
                    Mark as read
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default NotificationsPage;
