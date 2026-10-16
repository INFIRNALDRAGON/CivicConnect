import React from 'react';

interface EmptyStateProps {
  title: string;
  action?: React.ReactNode;
  children?: React.ReactNode;
}

const EmptyState: React.FC<EmptyStateProps> = ({ title, action, children }) => {
  return (
    <div className='mt-6 px-6 py-10 border-2 border-dashed border-line-strong rounded-xl text-center'>
      <h2 className='text-xl mb-2'>{title}</h2>
      {children && <p className='text-muted'>{children}</p>}
      {action}
    </div>
  );
};

export default EmptyState;
