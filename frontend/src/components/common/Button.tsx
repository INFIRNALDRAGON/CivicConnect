import React from 'react';

type Variant = 'primary' | 'secondary' | 'light';
type Size = 'sm' | 'md' | 'lg';

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ink text-white border-ink hover:bg-ink-soft hover:border-ink-soft',
  secondary: 'bg-white text-ink border-ink hover:bg-paper',
  light: 'bg-transparent text-white border-slate-400 hover:bg-ink-soft'
};

const SIZES: Record<Size, string> = {
  sm: 'min-h-10 px-3.5 text-[0.9375rem]',
  md: 'min-h-11 px-5',
  lg: 'min-h-[52px] px-7 text-lg'
};

export const buttonClass = (variant: Variant = 'primary', size: Size = 'md'): string =>
  `inline-flex items-center justify-center gap-2.5 border-2 rounded-lg font-bold no-underline cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed ${VARIANTS[variant]} ${SIZES[size]}`;

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const Button: React.FC<ButtonProps> = ({ variant = 'primary', size = 'md', loading = false, disabled, type = 'button', children, ...rest }) => {
  return (
    <button type={type} className={buttonClass(variant, size)} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading && <span className='inline-block w-[18px] h-[18px] border-[3px] border-current border-r-transparent rounded-full animate-spin' aria-hidden="true" />}
      <span>{children}</span>
    </button>
  );
};

export default Button;
