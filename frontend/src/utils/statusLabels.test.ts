import { describe, expect, it } from 'vitest';
import { Status } from '../types';
import {
  OPEN_STATUSES,
  REQUESTER_MESSAGES,
  STATUS_LABELS,
  STATUS_ORDER,
  TRANSITIONS,
  priorityLabel,
  statusLabel,
  statusMessage
} from './statusLabels';

const ALL: Status[] = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED'];

describe('status transitions (FR-010)', () => {
  it('defines a rule and a label and a message for every status', () => {
    ALL.forEach((status) => {
      expect(TRANSITIONS[status]).toBeDefined();
      expect(STATUS_LABELS[status].length).toBeGreaterThan(0);
      expect(REQUESTER_MESSAGES[status].length).toBeGreaterThan(0);
    });
  });

  it('follows the lifecycle one step at a time', () => {
    expect(TRANSITIONS.SUBMITTED).toEqual(['ASSIGNED', 'REJECTED']);
    expect(TRANSITIONS.ASSIGNED).toEqual(['IN_PROGRESS']);
    expect(TRANSITIONS.IN_PROGRESS).toEqual(['RESOLVED']);
    expect(TRANSITIONS.RESOLVED).toEqual(['CLOSED']);
  });

  it('never allows a jump over a stage', () => {
    ALL.forEach((from) => {
      TRANSITIONS[from].forEach((to) => {
        if (to === 'REJECTED') return;
        expect(STATUS_ORDER.indexOf(to) - STATUS_ORDER.indexOf(from)).toBe(1);
      });
    });
  });

  it('treats closed and rejected as final', () => {
    expect(TRANSITIONS.CLOSED).toEqual([]);
    expect(TRANSITIONS.REJECTED).toEqual([]);
  });

  it('only allows a rejection from the submitted stage', () => {
    const sources = ALL.filter((status) => TRANSITIONS[status].includes('REJECTED'));
    expect(sources).toEqual(['SUBMITTED']);
  });

  it('counts exactly the unfinished stages as open', () => {
    expect(OPEN_STATUSES).toEqual(['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS']);
  });
});

describe('labels', () => {
  it('uses plain words for statuses and priorities', () => {
    expect(statusLabel('IN_PROGRESS')).toBe('In progress');
    expect(statusMessage('REJECTED')).toContain('could not accept');
    expect(priorityLabel('MEDIUM')).toBe('Normal');
  });

  it('gives every status its own label', () => {
    expect(new Set(ALL.map((status) => STATUS_LABELS[status])).size).toBe(ALL.length);
  });
});
