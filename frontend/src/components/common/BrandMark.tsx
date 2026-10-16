import React from 'react';

const BrandMark: React.FC<{ size?: number }> = ({ size = 32 }) => {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false">
      <rect width="32" height="32" rx="7" fill="#f5b82e" />
      <path d="M8 22h6a4 4 0 0 0 4-4v-4a4 4 0 0 1 4-4h2" fill="none" stroke="#12263a" strokeWidth="2.6" strokeLinecap="round" />
      <circle cx="8" cy="22" r="2.6" fill="#12263a" />
      <circle cx="24" cy="10" r="2.6" fill="#12263a" />
    </svg>
  );
};

export default BrandMark;
