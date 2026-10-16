import { Role } from '../types';

export const ROLES: Record<Role, Role> = {
  REQUESTER: 'REQUESTER',
  STAFF: 'STAFF',
  MANAGER: 'MANAGER'
};

export const ROLE_LABELS: Record<Role, string> = {
  REQUESTER: 'Requester',
  STAFF: 'Staff',
  MANAGER: 'Management'
};

export const HOME_PATHS: Record<Role, string> = {
  REQUESTER: '/requests',
  STAFF: '/staff/queue',
  MANAGER: '/management/dashboard'
};

export const NAV_ITEMS: Record<Role, { to: string; label: string }[]> = {
  REQUESTER: [
    { to: '/submit', label: 'Submit a request' },
    { to: '/requests', label: 'My requests' },
    { to: '/notifications', label: 'Notifications' }
  ],
  STAFF: [{ to: '/staff/queue', label: 'Request queue' }],
  MANAGER: [{ to: '/management/dashboard', label: 'Service activity' }]
};

export const SIDEBAR_NOTES: Record<Role, string> = {
  REQUESTER: 'Never put passwords or ID numbers in a request description.',
  STAFF: 'You only see requests in the categories you are authorised for.',
  MANAGER: 'Reports show counts and request references only. Requester details are never shown.'
};

export const homePathFor = (role: Role): string => HOME_PATHS[role];
