import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useCategories } from '../../hooks/useRequests';
import { api, ApiError } from '../../utils/api';
import { NewRequest, ServiceRequest } from '../../types';
import Alert from '../../components/common/Alert';
import Button, { buttonClass } from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import RequestForm from '../../components/requests/RequestForm';
import { REF_TAG_CLASS } from '../../components/requests/RequestCard';

interface ConfirmationProps {
  request: ServiceRequest;
  onAnother: () => void;
}

const Confirmation: React.FC<ConfirmationProps> = ({ request, onAnother }) => {
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    heading.current?.focus();
  }, []);

  return (
    <div className='max-w-3xl'>
      <section aria-labelledby="confirmation-title">
        <h1 id="confirmation-title" className='text-3xl md:text-4xl' ref={heading} tabIndex={-1}>
          We have your request
        </h1>
        <p className='mt-2 max-w-[62ch] text-muted'>Keep this reference number. You can use it to follow your request at any time.</p>
        <p className={`${REF_TAG_CLASS} my-5 px-4 py-1.5 text-2xl`}>{request.referenceNumber}</p>
        <div className='flex flex-wrap gap-3 mt-2'>
          <Link className={buttonClass('primary', 'lg')} to={`/requests/${request.id}`}>
            View this request
          </Link>
          <Button variant="secondary" size="lg" onClick={onAnother}>
            Submit another request
          </Button>
        </div>
      </section>
    </div>
  );
};

const SubmitRequestPage: React.FC = () => {
  const { data: categories, loading, error, reload } = useCategories();
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const [created, setCreated] = useState<ServiceRequest | null>(null);
  const [formKey, setFormKey] = useState(0);

  const handleSubmit = async (payload: NewRequest) => {
    setSubmitting(true);
    setServerError('');
    try {
      setCreated(await api.post<ServiceRequest>('/requests', { ...payload }));
    } catch (err) {
      setServerError((err as ApiError).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (created) {
    return (
      <Confirmation
        request={created}
        onAnother={() => {
          setCreated(null);
          setFormKey((key) => key + 1);
        }}
      />
    );
  }

  return (
    <div className='max-w-3xl'>
      <h1 className='text-3xl md:text-4xl'>Submit a request</h1>
      <p className='mt-2 max-w-[62ch] text-muted'>Tell us what needs attention. It takes about two minutes.</p>

      {loading && !categories && <LoadingSpinner label="Loading categories" />}
      {error && (
        <Alert
          variant="error"
          title="Categories did not load"
          action={
            <Button variant="secondary" size="sm" onClick={reload}>
              Try again
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}
      {categories && <RequestForm key={formKey} categories={categories} onSubmit={handleSubmit} submitting={submitting} serverError={serverError} />}
    </div>
  );
};

export default SubmitRequestPage;
