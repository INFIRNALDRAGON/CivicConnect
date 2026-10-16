import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { NAV_ITEMS, SIDEBAR_NOTES } from '../../utils/roles';

interface SidebarProps {
  open: boolean;
  onNavigate: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ open, onNavigate }) => {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <aside
      id="sidebar"
      className={`fixed lg:static top-16 bottom-0 left-0 z-40 w-[250px] shrink-0 flex flex-col gap-6 px-4 py-6 bg-white border-r border-line transition-transform duration-200 ease-in-out ${open ? 'translate-x-0' : '-translate-x-full max-lg:invisible'} lg:translate-x-0`}
    >
      <nav className='flex flex-col gap-1' aria-label="Main">
        {NAV_ITEMS[user.role].map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `block px-3.5 py-3 border-l-[5px] rounded-r-lg font-bold no-underline hover:bg-paper ${isActive ? 'bg-paper border-signal text-ink' : 'border-transparent text-slate-900'}`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <p className='mt-auto mb-0 px-3.5 py-3 rounded-lg bg-blue-50 text-ink text-[0.9375rem]'>{SIDEBAR_NOTES[user.role]}</p>
    </aside>
  );
};

export default Sidebar;
