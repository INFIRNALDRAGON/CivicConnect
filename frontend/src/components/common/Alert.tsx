import React from 'react';

type Variant = 'info' | 'error' | 'success' | 'warning';

const VARIANTS: Record<Variant, string> = {
  info: 'bg-blue-50 border-ink text-ink',
  error: 'bg-red-50 border-red-700 text-red-950',
  success: 'bg-green-50 border-green-700 text-green-950',
  warning: 'bg-amber-50 border-signal text-amber-900'
};

interface AlertProps {
  variant?: Variant;
  title?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}

const Alert: React.FC<AlertProps> = ({ variant = 'info', title, action, children }) => {
  return (
    <div className={`flex flex-wrap items-center justify-between gap-3 my-4 p-4 border-l-[6px] rounded-md ${VARIANTS[variant]}`} role={variant === 'error' ? 'alert' : 'status'}>
      <div className='flex-1 basis-60'>
        {title && <strong className='block mb-0.5'>{title}</strong>}
        <div>{children}</div>
      </div>
      {action && <div>{action}</div>}
    </div>
  );
};

export default Alert;
