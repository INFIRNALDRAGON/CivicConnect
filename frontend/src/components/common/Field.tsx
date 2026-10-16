import React from 'react';

const CONTROL_BASE = 'w-full min-h-12 px-3.5 py-2.5 border-2 rounded-lg text-slate-900';

export const CONTROL_CLASS = `${CONTROL_BASE} border-line-strong bg-white`;

interface ControlProps {
  id?: string;
  className?: string;
  'aria-invalid'?: boolean;
  'aria-describedby'?: string;
}

interface FieldProps {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  optional?: boolean;
  children: React.ReactElement<ControlProps>;
}

const Field: React.FC<FieldProps> = ({ id, label, hint, error, optional = false, children }) => {
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined;
  return (
    <div className='flex flex-col gap-1.5 min-w-0'>
      <label htmlFor={id} className='font-bold'>
        {label}
        {optional && <span className='font-normal text-muted'> (optional)</span>}
      </label>
      {hint && (
        <p id={`${id}-hint`} className='m-0 text-[0.9375rem] text-muted'>
          {hint}
        </p>
      )}
      {React.cloneElement(children, {
        id,
        className: `${CONTROL_BASE} ${error ? 'border-red-700 bg-red-50' : 'border-line-strong bg-white'}`,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': describedBy
      })}
      {error && (
        <p id={`${id}-error`} className='m-0 text-[0.9375rem] font-bold text-red-700'>
          {error}
        </p>
      )}
    </div>
  );
};

export default Field;
