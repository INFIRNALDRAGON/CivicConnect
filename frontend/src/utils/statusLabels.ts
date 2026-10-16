import { Priority, Status } from '../types';

export const STATUS_ORDER: Status[] = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export const OPEN_STATUSES: Status[] = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS'];

export const STATUS_LABELS: Record<Status, string> = {
  SUBMITTED: 'Submitted',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  REJECTED: 'Rejected'
};

export const TRANSITIONS: Record<Status, Status[]> = {
  SUBMITTED: ['ASSIGNED', 'REJECTED'],
  ASSIGNED: ['IN_PROGRESS'],
  IN_PROGRESS: ['RESOLVED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  REJECTED: []
};

export const TRANSITION_ACTIONS: Partial<Record<Status, string>> = {
  IN_PROGRESS: 'Start work',
  REJECTED: 'Reject request',
  RESOLVED: 'Resolve request',
  CLOSED: 'Close request'
};

export const REQUESTER_MESSAGES: Record<Status, string> = {
  SUBMITTED: 'We have your request. A member of staff will pick it up soon.',
  ASSIGNED: 'A member of staff has taken responsibility for your request.',
  IN_PROGRESS: 'Work on your request has started.',
  RESOLVED: 'Your request has been resolved. The resolution note is in the history below.',
  CLOSED: 'This request is closed. If the problem comes back, submit a new request.',
  REJECTED: 'We could not accept this request. The reason is in the history below.'
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: 'Low',
  MEDIUM: 'Normal',
  HIGH: 'High'
};

export const PRIORITY_CHOICES: { value: Priority; label: string; hint: string }[] = [
  { value: 'LOW', label: 'Low', hint: 'Can wait' },
  { value: 'MEDIUM', label: 'Normal', hint: 'Needs attention soon' },
  { value: 'HIGH', label: 'High', hint: 'Safety risk or blocking work' }
];

export const statusLabel = (status: Status): string => STATUS_LABELS[status];

export const statusMessage = (status: Status): string => REQUESTER_MESSAGES[status];

export const priorityLabel = (priority: Priority): string => PRIORITY_LABELS[priority];
