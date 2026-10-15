import React, { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ApiError, USE_MOCK } from '../../utils/api';
import { homePathFor } from '../../utils/roles';
import AuthLayout from '../../components/layout/AuthLayout';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Field from '../../components/common/Field';

const DEMO_ACCOUNTS = [
  { label: 'Requester', email: 'requester@civicconnect.test' },
  { label: 'Staff', email: 'staff@civicconnect.test' },
  { label: 'Security staff', email: 'security@civicconnect.test' },
  { label: 'Management', email: 'manager@civicconnect.test' }
];

const LoginPage: React.FC = () => {
  const { user, login } = useAuth();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={location.state?.from || homePathFor(user.role)} replace />;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const found: { email?: string; password?: string } = {};
    if (!form.email.trim()) found.email = 'Enter your email address.';
    if (!form.password) found.password = 'Enter your password.';
    setErrors(found);
    setServerError('');
    const first = Object.keys(found)[0];
    if (first) {
      document.getElementById(`login-${first}`)?.focus();
      return;
    }
    setSubmitting(true);
    try {
      await login(form.email.trim(), form.password);
    } catch (error) {
      setServerError((error as ApiError).message);
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <h2 className='text-3xl'>Sign in</h2>
      {serverError && (
        <Alert variant="error" title="You are not signed in">
          {serverError}
        </Alert>
      )}

      <form className='grid gap-5 mt-6' onSubmit={handleSubmit} noValidate>
        <Field id="login-email" label="Email address" error={errors.email}>
          <input type="email" autoComplete="username" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
        </Field>
        <Field id="login-password" label="Password" error={errors.password}>
          <input type="password" autoComplete="current-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
        </Field>
        <Button type="submit" size="lg" loading={submitting}>
          Sign in
        </Button>
      </form>

      <p className='mt-6'>
        New to CivicConnect? <Link to="/register">Create a requester account</Link>
      </p>

      {/* Demo accounts */}
      {USE_MOCK && (
        <div className='mt-7 p-4 border-2 border-dashed border-line-strong rounded-xl'>
          <p className='m-0 font-bold'>Demo accounts (sample data, no server needed)</p>
          <p className='mt-1 mb-3 text-[0.9375rem] text-muted'>All demo accounts use the password Passw0rd!</p>
          <div className='flex flex-wrap gap-2'>
            {DEMO_ACCOUNTS.map((account) => (
              <Button key={account.email} variant="secondary" size="sm" onClick={() => setForm({ email: account.email, password: 'Passw0rd!' })}>
                {account.label}
              </Button>
            ))}
          </div>
        </div>
      )}
    </AuthLayout>
  );
};

export default LoginPage;
