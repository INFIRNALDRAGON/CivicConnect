import React, { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ApiError } from '../../utils/api';
import { homePathFor } from '../../utils/roles';
import AuthLayout from '../../components/layout/AuthLayout';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import Field from '../../components/common/Field';

type FormKey = 'name' | 'email' | 'password' | 'confirm';

const RegisterPage: React.FC = () => {
  const { user, register } = useAuth();
  const [form, setForm] = useState<Record<FormKey, string>>({ name: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState<Partial<Record<FormKey, string>>>({});
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={homePathFor(user.role)} replace />;

  const set = (key: FormKey) => (event: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: event.target.value });

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const found: Partial<Record<FormKey, string>> = {};
    if (!form.name.trim()) found.name = 'Enter your name.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) found.email = 'Enter a valid email address, like name@example.com.';
    if (form.password.length < 8) found.password = 'Use at least 8 characters.';
    if (form.confirm !== form.password) found.confirm = 'The two passwords do not match.';
    setErrors(found);
    setServerError('');
    const first = Object.keys(found)[0];
    if (first) {
      document.getElementById(`register-${first}`)?.focus();
      return;
    }
    setSubmitting(true);
    try {
      await register({ name: form.name.trim(), email: form.email.trim(), password: form.password });
    } catch (error) {
      setServerError((error as ApiError).message);
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <h2 className='text-3xl'>Create a requester account</h2>
      <p className='mt-2 text-muted'>Staff and management accounts are set up by your organisation.</p>
      {serverError && (
        <Alert variant="error" title="Your account was not created">
          {serverError}
        </Alert>
      )}

      <form className='grid gap-5 mt-6' onSubmit={handleSubmit} noValidate>
        <Field id="register-name" label="Full name" error={errors.name}>
          <input type="text" autoComplete="name" value={form.name} onChange={set('name')} />
        </Field>
        <Field id="register-email" label="Email address" error={errors.email}>
          <input type="email" autoComplete="email" value={form.email} onChange={set('email')} />
        </Field>
        <Field id="register-password" label="Password" hint="At least 8 characters." error={errors.password}>
          <input type="password" autoComplete="new-password" value={form.password} onChange={set('password')} />
        </Field>
        <Field id="register-confirm" label="Confirm password" error={errors.confirm}>
          <input type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} />
        </Field>
        <Button type="submit" size="lg" loading={submitting}>
          Create account
        </Button>
      </form>

      <p className='mt-6'>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthLayout>
  );
};

export default RegisterPage;
