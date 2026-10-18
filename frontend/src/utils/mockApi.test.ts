import { beforeEach, describe, expect, it } from 'vitest';
import { AppNotification, Report, ServiceRequest, Session } from '../types';
import { handleMockRequest } from './mockApi';

const PASSWORD = 'Test@1234';
const IT_SUPPORT = '4';
const LOST_PROPERTY = '6';

const login = async (email: string): Promise<string> => {
  const session = (await handleMockRequest('POST', '/auth/login', { email, password: PASSWORD })) as Session;
  return session.token;
};

const create = async (token: string, categoryId: string, description: string): Promise<ServiceRequest> =>
  (await handleMockRequest('POST', '/requests', { categoryId, description, priority: 'MEDIUM' }, {}, token)) as ServiceRequest;

const call = (method: string, path: string, token: string, body: Record<string, unknown> = {}, params: Record<string, string> = {}) =>
  handleMockRequest(method, path, body, params, token);

beforeEach(() => {
  localStorage.clear();
});

describe('sign in', () => {
  it('accepts every documented demo account', async () => {
    const accounts: Record<string, string> = {
      'manager@civicconnect.test': 'MANAGER',
      'staff1@civicconnect.test': 'STAFF',
      'staff2@civicconnect.test': 'STAFF',
      'staff3@civicconnect.test': 'STAFF',
      'req1@civicconnect.test': 'REQUESTER',
      'req2@civicconnect.test': 'REQUESTER',
      'req3@civicconnect.test': 'REQUESTER',
      'req4@civicconnect.test': 'REQUESTER',
      'req5@civicconnect.test': 'REQUESTER'
    };
    for (const [email, role] of Object.entries(accounts)) {
      const session = (await handleMockRequest('POST', '/auth/login', { email, password: PASSWORD })) as Session;
      expect(session.user.role).toBe(role);
    }
  });

  it('refuses a wrong password and an unknown account with the same message', async () => {
    const wrong = handleMockRequest('POST', '/auth/login', { email: 'req1@civicconnect.test', password: 'nope' });
    const unknown = handleMockRequest('POST', '/auth/login', { email: 'nobody@civicconnect.test', password: 'nope' });
    await expect(wrong).rejects.toMatchObject({ status: 401, message: 'Email or password is incorrect.' });
    await expect(unknown).rejects.toMatchObject({ status: 401, message: 'Email or password is incorrect.' });
  });

  it('registers new people as requesters only', async () => {
    const session = (await handleMockRequest('POST', '/auth/register', {
      name: 'New Person',
      email: 'new.person@civicconnect.test',
      password: 'Sufficient1pass',
      role: 'MANAGER'
    })) as Session;
    expect(session.user.role).toBe('REQUESTER');
    await expect(
      handleMockRequest('POST', '/auth/register', { name: 'Again', email: 'NEW.person@civicconnect.test', password: 'Sufficient1pass' })
    ).rejects.toMatchObject({ status: 409 });
  });

  it('rejects calls without a valid token', async () => {
    await expect(handleMockRequest('GET', '/requests/mine', {}, {}, undefined)).rejects.toMatchObject({ status: 401 });
    await expect(handleMockRequest('GET', '/requests/mine', {}, {}, 'garbage')).rejects.toMatchObject({ status: 401 });
  });
});

describe('submitting a request (FR-001, FR-002)', () => {
  it('enforces the ten character minimum after trimming', async () => {
    const token = await login('req1@civicconnect.test');
    await expect(create(token, IT_SUPPORT, '123456789')).rejects.toMatchObject({ status: 400 });
    await expect(create(token, IT_SUPPORT, '   123456789   ')).rejects.toMatchObject({ status: 400 });
    const accepted = await create(token, IT_SUPPORT, '   1234567890   ');
    expect(accepted.description).toBe('1234567890');
    expect(accepted.status).toBe('SUBMITTED');
    expect(accepted.referenceNumber).toMatch(/^CC-/);
  });

  it('refuses a missing or unknown category', async () => {
    const token = await login('req1@civicconnect.test');
    await expect(create(token, '', 'The tap in the hall is dripping')).rejects.toMatchObject({ status: 400 });
    await expect(create(token, '99', 'The tap in the hall is dripping')).rejects.toMatchObject({ status: 400 });
  });

  it('does not let staff submit requests', async () => {
    const token = await login('staff1@civicconnect.test');
    await expect(create(token, IT_SUPPORT, 'Staff should not submit this one')).rejects.toMatchObject({ status: 403 });
  });

  it('gives every request a distinct reference', async () => {
    const token = await login('req1@civicconnect.test');
    const references = new Set<string>();
    for (let i = 0; i < 5; i += 1) {
      references.add((await create(token, IT_SUPPORT, `Reference uniqueness check number ${i}`)).referenceNumber);
    }
    expect(references.size).toBe(5);
  });
});

describe('who can see what (FR-006, NFR-003)', () => {
  it('keeps one requester out of another requester\'s request', async () => {
    const owner = await login('req1@civicconnect.test');
    const other = await login('req2@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'A private request about my desk');
    await expect(call('GET', `/requests/${created.id}`, other)).rejects.toMatchObject({ status: 404 });
    const mine = (await call('GET', '/requests/mine', other)) as ServiceRequest[];
    expect(mine.some((item) => item.id === created.id)).toBe(false);
  });

  it('limits staff to their own categories', async () => {
    const owner = await login('req1@civicconnect.test');
    const lost = await create(owner, LOST_PROPERTY, 'I left my umbrella in the main hall');
    const staff1 = await login('staff1@civicconnect.test');
    const staff3 = await login('staff3@civicconnect.test');
    await expect(call('GET', `/requests/${lost.id}`, staff1)).rejects.toMatchObject({ status: 403 });
    await expect(call('POST', `/requests/${lost.id}/assign`, staff1, { version: lost.version })).rejects.toMatchObject({ status: 403 });
    const queue1 = (await call('GET', '/requests', staff1)) as ServiceRequest[];
    expect(queue1.every((item) => ['IT support', 'Maintenance'].includes(item.categoryName))).toBe(true);
    const seen = (await call('GET', `/requests/${lost.id}`, staff3)) as ServiceRequest;
    expect(seen.id).toBe(lost.id);
  });

  it('keeps staff names and internal fields away from requesters', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'Projector will not switch on today');
    await call('POST', `/requests/${created.id}/assign`, staff, { version: created.version });
    const seen = (await call('GET', `/requests/${created.id}`, owner)) as ServiceRequest;
    expect(seen.status).toBe('ASSIGNED');
    expect(seen.assignedStaffName).toBeUndefined();
    expect(seen.requesterReference).toBeUndefined();
    expect(JSON.stringify(seen)).not.toContain('Sam Staff-One');
  });

  it('keeps the staff queue and reports closed to requesters', async () => {
    const owner = await login('req1@civicconnect.test');
    await expect(call('GET', '/requests', owner)).rejects.toMatchObject({ status: 403 });
    await expect(call('GET', '/reports/summary', owner)).rejects.toMatchObject({ status: 403 });
    const staff = await login('staff1@civicconnect.test');
    await expect(call('GET', '/reports/summary', staff)).rejects.toMatchObject({ status: 403 });
  });
});

describe('working a request (FR-009 to FR-012)', () => {
  it('moves one stage at a time and needs a note to resolve', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'Printer on the second floor jams constantly');

    await expect(call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'RESOLVED', comment: 'Fixed', version: created.version })).rejects.toMatchObject({ status: 400 });

    const assigned = (await call('POST', `/requests/${created.id}/assign`, staff, { version: created.version })) as ServiceRequest;
    expect(assigned.status).toBe('ASSIGNED');
    await expect(call('POST', `/requests/${created.id}/assign`, staff, { version: assigned.version })).rejects.toMatchObject({ status: 400 });

    const started = (await call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'IN_PROGRESS', version: assigned.version })) as ServiceRequest;
    await expect(call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'RESOLVED', comment: '   ', version: started.version })).rejects.toMatchObject({ status: 400 });

    const resolved = (await call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'RESOLVED', comment: 'Replaced the roller', version: started.version })) as ServiceRequest;
    expect(resolved.status).toBe('RESOLVED');
    expect(resolved.resolvedAt).not.toBeNull();
    await expect(call('POST', `/requests/${created.id}/comments`, staff, { comment: 'One more thing', version: resolved.version })).rejects.toMatchObject({ status: 400 });
  });

  it('treats a stale version as a conflict', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'Stale version check for the queue');
    await call('POST', `/requests/${created.id}/assign`, staff, { version: created.version });
    await expect(call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'IN_PROGRESS', version: created.version })).rejects.toMatchObject({ status: 409 });
  });

  it('needs a reason to reject and then treats the request as finished', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'Duplicate of the earlier printer report');
    await expect(call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'REJECTED', version: created.version })).rejects.toMatchObject({ status: 400 });
    const rejected = (await call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'REJECTED', comment: 'Duplicate report', version: created.version })) as ServiceRequest;
    expect(rejected.status).toBe('REJECTED');
    expect(rejected.allowedTransitions).toEqual([]);
    await expect(call('POST', `/requests/${created.id}/assign`, staff, { version: rejected.version })).rejects.toMatchObject({ status: 400 });
  });

  it('records every change in order in the history', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'History order check for the router');
    const assigned = (await call('POST', `/requests/${created.id}/assign`, staff, { version: created.version })) as ServiceRequest;
    await call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'IN_PROGRESS', version: assigned.version });
    const seen = (await call('GET', `/requests/${created.id}`, owner)) as ServiceRequest;
    expect(seen.history?.map((entry) => entry.newStatus)).toEqual(['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS']);
  });
});

describe('notifications (FR-005)', () => {
  it('tells the requester about each change and remembers what was read', async () => {
    const owner = await login('req1@civicconnect.test');
    const staff = await login('staff1@civicconnect.test');
    const created = await create(owner, IT_SUPPORT, 'Notification check for the front desk');
    const assigned = (await call('POST', `/requests/${created.id}/assign`, staff, { version: created.version })) as ServiceRequest;
    await call('POST', `/requests/${created.id}/status`, staff, { targetStatus: 'IN_PROGRESS', version: assigned.version });

    const list = (await call('GET', '/notifications', owner)) as AppNotification[];
    const mine = list.filter((item) => item.referenceNumber === created.referenceNumber);
    expect(mine.length).toBe(3);
    expect(mine.every((item) => item.readAt === null)).toBe(true);

    await call('POST', `/notifications/${mine[0].id}/read`, owner);
    const after = (await call('GET', '/notifications', owner)) as AppNotification[];
    expect(after.find((item) => item.id === mine[0].id)?.readAt).not.toBeNull();

    const other = await login('req2@civicconnect.test');
    const theirs = (await call('GET', '/notifications', other)) as AppNotification[];
    expect(theirs.some((item) => item.referenceNumber === created.referenceNumber)).toBe(false);
  });
});

describe('management summary (FR-013 to FR-016)', () => {
  it('counts the seeded requests and never shows requester details', async () => {
    const manager = await login('manager@civicconnect.test');
    const report = (await call('GET', '/reports/summary', manager)) as Report;
    expect(report.total).toBe(12);
    expect(Object.values(report.totals).reduce((sum, value) => sum + value, 0)).toBe(12);
    expect(JSON.stringify(report)).not.toContain('@civicconnect.test');
    expect(JSON.stringify(report)).not.toContain('Riley');
  });

  it('can be narrowed to one category', async () => {
    const manager = await login('manager@civicconnect.test');
    const report = (await call('GET', '/reports/summary', manager, {}, { categoryId: IT_SUPPORT })) as Report;
    expect(report.total).toBeGreaterThan(0);
    expect(report.byCategory.every((item) => item.categoryName === 'IT support')).toBe(true);
  });
});
