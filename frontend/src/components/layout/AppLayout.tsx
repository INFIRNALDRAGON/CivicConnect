import React, { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

const AppLayout: React.FC = () => {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setMenuOpen(false);
    window.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  return (
    <div className='min-h-screen flex flex-col'>
      <a className='absolute left-3 -top-16 focus:top-2.5 z-50 px-4 py-2.5 rounded-md bg-signal text-ink font-bold no-underline' href="#main">
        Skip to main content
      </a>

      {/* Navbar */}
      <Navbar menuOpen={menuOpen} onToggleMenu={() => setMenuOpen((open) => !open)} />

      <div className='flex flex-1 min-h-0'>
        {/* Sidebar */}
        <Sidebar open={menuOpen} onNavigate={() => setMenuOpen(false)} />
        {menuOpen && <div className='lg:hidden fixed inset-x-0 top-16 bottom-0 z-[35] bg-ink/50' onClick={() => setMenuOpen(false)} aria-hidden="true" />}

        {/* Page content */}
        <main id="main" className='flex-1 min-w-0 px-4 sm:px-8 lg:px-12 pt-8 pb-16' tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
