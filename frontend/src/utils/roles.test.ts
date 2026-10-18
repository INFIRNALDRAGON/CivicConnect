import { describe, expect, it } from 'vitest';
import { HOME_PATHS, NAV_ITEMS, homePathFor } from './roles';

describe('role navigation', () => {
  it('sends each role to its own home page', () => {
    expect(homePathFor('REQUESTER')).toBe('/requests');
    expect(homePathFor('STAFF')).toBe('/staff/queue');
    expect(homePathFor('MANAGER')).toBe('/management/dashboard');
  });

  it('keeps every menu entry inside the area of its role', () => {
    NAV_ITEMS.STAFF.forEach((item) => expect(item.to.startsWith('/staff')).toBe(true));
    NAV_ITEMS.MANAGER.forEach((item) => expect(item.to.startsWith('/management')).toBe(true));
    NAV_ITEMS.REQUESTER.forEach((item) => expect(item.to.startsWith('/staff') || item.to.startsWith('/management')).toBe(false));
  });

  it('offers notifications to requesters only', () => {
    expect(NAV_ITEMS.REQUESTER.some((item) => item.to === '/notifications')).toBe(true);
    expect(NAV_ITEMS.STAFF.some((item) => item.to === '/notifications')).toBe(false);
    expect(NAV_ITEMS.MANAGER.some((item) => item.to === '/notifications')).toBe(false);
  });

  it('has a home path for every role in the menu', () => {
    (Object.keys(NAV_ITEMS) as (keyof typeof NAV_ITEMS)[]).forEach((role) => expect(HOME_PATHS[role]).toBeTruthy());
  });
});
