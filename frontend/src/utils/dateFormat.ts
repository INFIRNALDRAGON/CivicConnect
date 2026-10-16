const TIME_ZONE = 'Africa/Johannesburg';

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  timeZone: TIME_ZONE,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
});

const dateFormat = new Intl.DateTimeFormat('en-GB', {
  timeZone: TIME_ZONE,
  day: 'numeric',
  month: 'short',
  year: 'numeric'
});

const plural = (value: number, unit: string): string => `${value} ${unit}${value === 1 ? '' : 's'}`;

export const formatDateTime = (iso?: string | null): string => (iso ? dateTimeFormat.format(new Date(iso)) : '-');

export const formatDate = (iso?: string | null): string => (iso ? dateFormat.format(new Date(iso)) : '-');

export const ageInHours = (iso: string, now: number = Date.now()): number =>
  Math.max(0, (now - new Date(iso).getTime()) / 3600000);

export const formatAge = (iso: string, now: number = Date.now()): string => {
  const hours = Math.floor(ageInHours(iso, now));
  if (hours < 1) return 'Under an hour';
  if (hours < 48) return plural(hours, 'hour');
  return plural(Math.floor(hours / 24), 'day');
};

export const formatAgo = (iso: string, now: number = Date.now()): string => {
  const hours = Math.floor(ageInHours(iso, now));
  return hours < 1 ? 'just now' : `${formatAge(iso, now)} ago`;
};

export const formatDuration = (hours: number | null): string => {
  if (hours === null) return '-';
  if (hours < 1) return `${Math.max(1, Math.round(hours * 60))} min`;
  if (hours < 48) return `${hours.toFixed(1)} hours`;
  return `${(hours / 24).toFixed(1)} days`;
};

export const dateInputValue = (offsetDays: number = 0): string =>
  new Date(Date.now() - offsetDays * 86400000).toLocaleDateString('en-CA', { timeZone: TIME_ZONE });
