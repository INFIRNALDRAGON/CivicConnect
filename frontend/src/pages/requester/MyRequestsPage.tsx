import React from 'react';
import { Link } from 'react-router-dom';
import { useMyRequests } from '../../hooks/useRequests';
import Alert from '../../components/common/Alert';
import Button, { buttonClass } from '../../components/common/Button';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import RequestCard from '../../components/requests/RequestCard';

const MyRequestsPage: React.FC = () => {
  const { data: requests, loading, error, reload } = useMyRequests(30000);

  return (
    <div className='max-w-3xl'>
      {/* Heading */}
      <div className='flex flex-wrap items-start justify-between gap-4'>
        <div>
          <h1 className='text-3xl md:text-4xl'>My requests</h1>
          <p className='mt-2 max-w-[62ch] text-muted'>Every request you have submitted, newest first.</p>
        </div>
        <Link className={buttonClass('primary', 'md')} to="/submit">
          Submit a request
        </Link>
      </div>

      {loading && !requests && <LoadingSpinner label="Loading your requests" />}
      {error && (
        <Alert
          variant="error"
          title="Your requests did not load"
          action={
            <Button variant="secondary" size="sm" onClick={reload}>
              Try again
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}
      {requests && requests.length === 0 && (
        <EmptyState
          title="You have not submitted a request yet"
          action={
            <Link className={buttonClass('primary', 'md')} to="/submit">
              Submit your first request
            </Link>
          }
        >
          When you do, it will appear here with its status.
        </EmptyState>
      )}
      {requests && requests.length > 0 && (
        <ul className='grid gap-3 mt-4 p-0 list-none' aria-label="Your requests">
          {requests.map((request) => (
            <RequestCard key={request.id} request={request} to={`/requests/${request.id}`} />
          ))}
        </ul>
      )}
    </div>
  );
};

export default MyRequestsPage;
