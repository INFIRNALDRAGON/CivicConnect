import { describe, expect, it } from 'vitest';
import { ageInHours, formatAge, formatAgo, formatDate, formatDateTime, formatDuration } from './dateFormat';

const NOW = Date.parse('2026-10-10T12:00:00Z');
const hoursBefore = (hours: number): string => new Date(NOW - hours * 3600000).toISOString();

describe('formatAge', () => {
  it('switches wording at the hour and day boundaries', () => {
    expect(formatAge(hoursBefore(0.99), NOW)).toBe('Under an hour');
    expect(formatAge(hoursBefore(1), NOW)).toBe('1 hour');
    expect(formatAge(hoursBefore(47), NOW)).toBe('47 hours');
    expect(formatAge(hoursBefore(48), NOW)).toBe('2 days');
    expect(formatAge(hoursBefore(72), NOW)).toBe('3 days');
  });

  it('never reports a negative age', () => {
    expect(ageInHours(hoursBefore(-5), NOW)).toBe(0);
  });
});

describe('formatAgo', () => {
  it('says just now for anything under an hour', () => {
    expect(formatAgo(hoursBefore(0.2), NOW)).toBe('just now');
    expect(formatAgo(hoursBefore(5), NOW)).toBe('5 hours ago');
  });
});

describe('formatDuration', () => {
  it('chooses minutes, hours or days', () => {
    expect(formatDuration(null)).toBe('-');
    expect(formatDuration(0.5)).toBe('30 min');
    expect(formatDuration(5)).toBe('5.0 hours');
    expect(formatDuration(72)).toBe('3.0 days');
  });
});

describe('South African time', () => {
  it('shows an instant in SAST, two hours ahead of UTC', () => {
    expect(formatDateTime('2026-10-01T22:30:00Z')).toContain('2 Oct 2026');
    expect(formatDateTime('2026-10-01T22:30:00Z')).toContain('00:30');
    expect(formatDate('2026-10-01T22:30:00Z')).toBe('2 Oct 2026');
  });

  it('shows a dash for a missing date', () => {
    expect(formatDateTime(null)).toBe('-');
    expect(formatDate(undefined)).toBe('-');
  });
});
