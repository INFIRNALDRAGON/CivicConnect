import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ROLE_LABELS, homePathFor } from '../../utils/roles';
import BrandMark from '../common/BrandMark';
import Button from '../common/Button';

interface NavbarProps {
  menuOpen: boolean;
  onToggleMenu: () => void;
}

const Navbar: React.FC<NavbarProps> = ({ menuOpen, onToggleMenu }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const handleSignOut = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className='sticky top-0 z-50 h-16 flex items-center gap-4 px-3 md:px-5 bg-ink text-white'>
      <button
        type="button"
        className='lg:hidden inline-flex items-center justify-center w-11 h-11 rounded-lg text-white cursor-pointer'
        aria-label={menuOpen ? 'Close menu' : 'Open menu'}
        aria-expanded={menuOpen}
        aria-controls="sidebar"
        onClick={onToggleMenu}
      >
        <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true" focusable="false">
          <path d="M3 6h16M3 11h16M3 16h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      </button>

      <Link to={homePathFor(user.role)} className='inline-flex items-center gap-2.5 font-display font-extrabold text-xl text-white no-underline'>
        <BrandMark size={30} />
        <span>CivicConnect</span>
      </Link>

      <div className='ml-auto hidden sm:flex flex-col text-right leading-tight'>
        <span className='font-bold'>{user.name}</span>
        <span className='text-sm text-slate-300'>{ROLE_LABELS[user.role]}</span>
      </div>

      <div className='ml-auto sm:ml-0'>
        <Button variant="light" size="sm" onClick={handleSignOut}>
          Sign out
        </Button>
      </div>
    </header>
  );
};

export default Navbar;
