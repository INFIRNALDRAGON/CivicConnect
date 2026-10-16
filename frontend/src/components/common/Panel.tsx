import React from 'react';

interface PanelProps {
  as?: 'section' | 'aside' | 'div';
  className?: string;
  labelledBy?: string;
  label?: string;
  children: React.ReactNode;
}

const Panel: React.FC<PanelProps> = ({ as: Tag = 'section', className = '', labelledBy, label, children }) => {
  return (
    <Tag className={`mt-5 p-6 bg-white border border-line rounded-xl ${className}`} aria-labelledby={labelledBy} aria-label={label}>
      {children}
    </Tag>
  );
};

export default Panel;
