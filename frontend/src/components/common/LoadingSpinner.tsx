import React from 'react';

const LoadingSpinner: React.FC<{ label?: string }> = ({ label = 'Loading' }) => {
  return (
    <div className='flex items-center gap-3 py-8 text-muted' role="status">
      <span className='inline-block w-[18px] h-[18px] border-[3px] border-current border-r-transparent rounded-full animate-spin' aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
};

export default LoadingSpinner;
