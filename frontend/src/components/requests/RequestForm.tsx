import React, { useState } from 'react';
import { Category, NewRequest, Priority } from '../../types';
import Alert from '../common/Alert';
import Button from '../common/Button';
import Field from '../common/Field';
import { PRIORITY_CHOICES } from '../../utils/statusLabels';

const MIN_DESCRIPTION = 10;

interface RequestFormProps {
  categories: Category[];
  onSubmit: (payload: NewRequest) => void;
  submitting: boolean;
  serverError: string;
}

interface FormValues {
  categoryId: string;
  priority: Priority;
  description: string;
}

const RequestForm: React.FC<RequestFormProps> = ({ categories, onSubmit, submitting, serverError }) => {
  const [values, setValues] = useState<FormValues>({ categoryId: '', priority: 'MEDIUM', description: '' });
  const [errors, setErrors] = useState<Partial<Record<keyof FormValues, string>>>({});

  const update = (patch: Partial<FormValues>) => {
    setValues({ ...values, ...patch });
    setErrors({ ...errors, ...Object.fromEntries(Object.keys(patch).map((key) => [key, undefined])) });
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const found: Partial<Record<keyof FormValues, string>> = {};
    if (!values.categoryId) found.categoryId = 'Choose a category so the right team sees your request.';
    if (values.description.trim().length < MIN_DESCRIPTION) {
      found.description = `Describe the problem in at least ${MIN_DESCRIPTION} characters.`;
    }
    setErrors(found);
    const first = Object.keys(found)[0];
    if (first) {
      document.getElementById(`request-${first}`)?.focus();
      return;
    }
    onSubmit({ categoryId: values.categoryId, priority: values.priority, description: values.description.trim() });
  };

  return (
    <form className='grid gap-5 mt-6 max-w-[640px]' onSubmit={handleSubmit} noValidate>
      {serverError && (
        <Alert variant="error" title="Your request was not submitted">
          {serverError}
        </Alert>
      )}

      {/* Category */}
      <Field id="request-categoryId" label="Category" error={errors.categoryId}>
        <select value={values.categoryId} onChange={(event) => update({ categoryId: event.target.value })}>
          <option value="">Choose a category</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </Field>

      {/* Urgency */}
      <fieldset className='m-0 p-0 border-0 min-w-0'>
        <legend className='mb-2 p-0 font-bold'>How urgent is it?</legend>
        <div className='grid sm:grid-cols-3 gap-2.5'>
          {PRIORITY_CHOICES.map((choice) => (
            <label key={choice.value} className={`flex items-start gap-2.5 p-3 border-2 rounded-lg cursor-pointer ${values.priority === choice.value ? 'border-ink bg-blue-50' : 'border-line-strong bg-white'}`}>
              <input type="radio" name="priority" className='w-5 h-5 mt-0.5 accent-ink' checked={values.priority === choice.value} onChange={() => update({ priority: choice.value })} />
              <span className='flex flex-col'>
                <span className='font-bold'>{choice.label}</span>
                <span className='text-sm leading-snug text-muted'>{choice.hint}</span>
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      {/* Description */}
      <Field id="request-description" label="What is the problem?" hint="Say where it is and what you have noticed. Do not include passwords or ID numbers." error={errors.description}>
        <textarea rows={6} maxLength={2000} value={values.description} onChange={(event) => update({ description: event.target.value })} />
      </Field>
      <p className='-mt-3 mb-0 text-[0.9375rem] text-muted' aria-live="polite">
        {values.description.trim().length} of at least {MIN_DESCRIPTION} characters
      </p>

      <div className='mt-2'>
        <Button type="submit" size="lg" loading={submitting}>
          Submit request
        </Button>
      </div>
    </form>
  );
};

export default RequestForm;
