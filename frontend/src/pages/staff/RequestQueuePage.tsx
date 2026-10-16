import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { useCategories, useQueue } from '../../hooks/useRequests';
import { QueueFilters } from '../../types';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import RequestCard from '../../components/requests/RequestCard';
import RequestFilterBar from '../../components/requests/RequestFilterBar';

const KEYS: (keyof QueueFilters)[] = ['search', 'categoryId', 'status', 'sort'];

const RequestQueuePage: React.FC = () => {
  const [params, setParams] = useSearchParams();
  const filters: QueueFilters = {
    search: params.get('search') || '',
    categoryId: params.get('categoryId') || '',
    status: params.get('status') || '',
    sort: params.get('sort') || 'age_desc'
  };
  const { data: categories } = useCategories();
  const { data: requests, loading, error, reload } = useQueue(filters, 60000);

  const update = (patch: Partial<QueueFilters>) => {
    const next = new URLSearchParams();
    const merged = { ...filters, ...patch };
    KEYS.forEach((key) => {
      if (merged[key] && !(key === 'sort' && merged[key] === 'age_desc')) next.set(key, merged[key]);
    });
    setParams(next, { replace: true });
  };

  const overdue = requests ? requests.filter((request) => request.overdue).length : 0;

  return (
    <div className='max-w-6xl'>
      <h1 className='text-3xl md:text-4xl'>Request queue</h1>
      <p className='mt-2 max-w-[62ch] text-muted'>Requests in the categories you are authorised for.</p>

      <RequestFilterBar values={filters} categories={categories} onChange={update} onReset={() => setParams({}, { replace: true })} />

      {loading && !requests && <LoadingSpinner label="Loading the queue" />}
      {error && (
        <Alert
          variant="error"
          title="The queue did not load"
          action={
            <Button variant="secondary" size="sm" onClick={reload}>
              Try again
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}
      {requests && (
        <>
          <p className='mt-5 mb-0 font-bold' aria-live="polite">
            {requests.length} {requests.length === 1 ? 'request' : 'requests'}
            {overdue > 0 && <span className='text-red-700'> and {overdue} overdue</span>}
          </p>
          {requests.length === 0 ? (
            <EmptyState title="No requests match">Change or clear the filters to see more of the queue.</EmptyState>
          ) : (
            <ul className='grid gap-3 mt-4 p-0 list-none' aria-label="Requests">
              {requests.map((request) => (
                <RequestCard key={request.id} request={request} to={`/staff/requests/${request.id}`} audience="staff" />
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
};

export default RequestQueuePage;
