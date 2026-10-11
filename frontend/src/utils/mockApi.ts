import { ApiError, Body, Params } from './api';
import { HistoryEntry, Priority, Report, Role, ServiceRequest, Status } from '../types';
import { OPEN_STATUSES, STATUS_LABELS, STATUS_ORDER, TRANSITIONS } from './statusLabels';

interface MockUser {
  id: number;
  name: string;
  email: string;
  password: string;
  role: Role;
  categoryIds: number[];
}

interface MockCategory {
  id: number;
  name: string;
  overdueAfterHours: number;
}

interface MockEntry {
  id: number;
  previousStatus: Status | null;
  newStatus: Status;
  changedById: number;
  changedAt: string;
  comment: string | null;
}

interface MockRequest {
  id: number;
  referenceNumber: string;
  categoryId: number;
  description: string;
  status: Status;
  priority: Priority;
  requesterId: number;
  assignedStaffId: number | null;
  assignedAt: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  version: number;
  history: MockEntry[];
}

interface MockDb {
  seq: { request: number; history: number; user: number };
  categories: MockCategory[];
  users: MockUser[];
  requests: MockRequest[];
}

interface RouteInput {
  db: MockDb;
  token?: string;
  body: Body;
  params: Params;
  args: string[];
}

interface SecuredInput extends RouteInput {
  user: MockUser;
}

interface Route {
  method: string;
  pattern: RegExp;
  run: (input: RouteInput) => unknown;
}

const STORAGE_KEY = 'cc_mock_db_v1';
const LATENCY_MS = 220;
const PRIORITIES: Priority[] = ['LOW', 'NORMAL', 'HIGH'];
const NOTES: Partial<Record<Status, string>> = {
  RESOLVED: 'The fault has been repaired and tested.',
  CLOSED: 'Confirmed with the requester. Closing this request.'
};

const hoursAgo = (hours: number): string => new Date(Date.now() - hours * 3600000).toISOString();
const pad = (value: number, length = 6): string => String(value).padStart(length, '0');
const isPriority = (value: unknown): value is Priority => PRIORITIES.includes(value as Priority);
const hoursBetween = (from: string, to: string): number => (Date.parse(to) - Date.parse(from)) / 3600000;

function fail(status: number, message: string): never {
  throw new ApiError(status, message);
}

const seedDb = (): MockDb => {
  const db: MockDb = {
    seq: { request: 1, history: 1, user: 6 },
    categories: [
      { id: 1, name: 'Facility fault', overdueAfterHours: 72 },
      { id: 2, name: 'Damaged equipment', overdueAfterHours: 96 },
      { id: 3, name: 'Security concern', overdueAfterHours: 24 },
      { id: 4, name: 'IT support', overdueAfterHours: 48 },
      { id: 5, name: 'Maintenance', overdueAfterHours: 120 },
      { id: 6, name: 'Lost property', overdueAfterHours: 168 },
      { id: 7, name: 'Other', overdueAfterHours: 120 }
    ],
    users: [
      { id: 1, name: 'Thandi Mokoena', email: 'requester@civicconnect.test', password: 'Passw0rd!', role: 'REQUESTER', categoryIds: [] },
      { id: 2, name: 'Pieter Naidoo', email: 'staff@civicconnect.test', password: 'Passw0rd!', role: 'STAFF', categoryIds: [1, 2, 4, 5, 6, 7] },
      { id: 3, name: 'Lerato Dlamini', email: 'security@civicconnect.test', password: 'Passw0rd!', role: 'STAFF', categoryIds: [3] },
      { id: 4, name: 'Anele Jacobs', email: 'manager@civicconnect.test', password: 'Passw0rd!', role: 'MANAGER', categoryIds: [] },
      { id: 5, name: 'Sipho Khumalo', email: 'requester2@civicconnect.test', password: 'Passw0rd!', role: 'REQUESTER', categoryIds: [] }
    ],
    requests: []
  };

  const add = (
    categoryId: number,
    requesterId: number,
    description: string,
    status: Status,
    priority: Priority,
    age: number,
    staffId = 0,
    note?: string
  ): void => {
    const id = db.seq.request++;
    const index = STATUS_ORDER.indexOf(status);
    const step = age / (index + 1);
    const history: MockEntry[] = STATUS_ORDER.slice(0, index + 1).map((stage, i) => ({
      id: db.seq.history++,
      previousStatus: i ? STATUS_ORDER[i - 1] : null,
      newStatus: stage,
      changedById: i ? staffId : requesterId,
      changedAt: hoursAgo(age - step * i),
      comment: NOTES[stage] || null
    }));
    if (note) {
      history.push({
        id: db.seq.history++,
        previousStatus: status,
        newStatus: status,
        changedById: staffId,
        changedAt: hoursAgo(4),
        comment: note
      });
    }
    db.requests.push({
      id,
      referenceNumber: `CC-${new Date(history[0].changedAt).getFullYear()}-${pad(id)}`,
      categoryId,
      description,
      status,
      priority,
      requesterId,
      assignedStaffId: index >= 1 ? staffId : null,
      assignedAt: index >= 1 ? history[1].changedAt : null,
      createdAt: history[0].changedAt,
      updatedAt: history[history.length - 1].changedAt,
      resolvedAt: index >= 3 ? history[3].changedAt : null,
      version: history.length,
      history
    });
  };

  add(1, 1, 'The lift on the second floor stops between floors and the doors stay shut.', 'IN_PROGRESS', 'HIGH', 100, 2);
  add(4, 1, 'My workstation in the front office will not connect to the shared printer.', 'ASSIGNED', 'NORMAL', 20, 2);
  add(6, 1, 'I lost a blue backpack in the hall after the community meeting on Saturday.', 'SUBMITTED', 'LOW', 6);
  add(5, 5, 'A tap in the ground-floor bathroom has been dripping for a week.', 'RESOLVED', 'NORMAL', 60, 2);
  add(3, 5, 'The side gate latch is broken and the gate does not lock at night.', 'SUBMITTED', 'HIGH', 30);
  add(2, 1, 'Two folding tables in the hall have cracked legs and are unsafe to use.', 'CLOSED', 'NORMAL', 200, 2);
  add(1, 5, 'Several lights in the car park are not working after dark.', 'SUBMITTED', 'NORMAL', 2);
  add(4, 5, 'Wi-Fi drops every few minutes in the meeting room.', 'IN_PROGRESS', 'NORMAL', 30, 2, 'Waiting for the network supplier to confirm a visit.');
  add(5, 1, 'The roof leaks above the storage room when it rains.', 'ASSIGNED', 'HIGH', 140, 2);
  add(7, 5, 'Please add a sign at the entrance showing the wheelchair ramp.', 'CLOSED', 'LOW', 300, 2);
  add(3, 1, 'A stranger was seen trying door handles in the east corridor.', 'IN_PROGRESS', 'HIGH', 10, 3);
  add(4, 1, 'Email on the shared reception account will not sync.', 'RESOLVED', 'NORMAL', 90, 2);
  return db;
};

let cache: MockDb | null = null;

const saveDb = (): void => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cache));
  } catch {
    return;
  }
};

const loadDb = (): MockDb => {
  if (cache) return cache;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const stored = JSON.parse(raw) as MockDb;
      cache = stored;
      return stored;
    }
  } catch {
    cache = null;
  }
  const fresh = seedDb();
  cache = fresh;
  saveDb();
  return fresh;
};

const publicUser = ({ id, name, email, role }: MockUser) => ({ id, name, email, role });

const findCategory = (db: MockDb, id: number): MockCategory => {
  const category = db.categories.find((item) => item.id === id);
  return category ?? fail(500, 'The request refers to a category that does not exist.');
};

const authenticate = (db: MockDb, token?: string): MockUser => {
  const match = /^mock\.(\d+)$/.exec(token || '');
  const user = match ? db.users.find((candidate) => candidate.id === Number(match[1])) : undefined;
  return user ?? fail(401, 'Your session has ended. Sign in again to continue.');
};

const requireRole = (user: MockUser, ...roles: Role[]): void => {
  if (!roles.includes(user.role)) fail(403, 'You do not have permission to do that.');
};

const inScope = (user: MockUser, request: MockRequest): boolean => user.categoryIds.includes(request.categoryId);

const isOverdue = (db: MockDb, request: MockRequest): boolean => {
  const limit = findCategory(db, request.categoryId).overdueAfterHours;
  const ageHours = hoursBetween(request.createdAt, new Date().toISOString());
  return OPEN_STATUSES.includes(request.status) && ageHours > limit;
};

const toDto = (db: MockDb, request: MockRequest, viewer: MockUser, withHistory = false): ServiceRequest => {
  const isStaff = viewer.role === 'STAFF';
  const owner = db.users.find((item) => item.id === request.assignedStaffId);
  const dto: ServiceRequest = {
    id: request.id,
    referenceNumber: request.referenceNumber,
    categoryId: request.categoryId,
    categoryName: findCategory(db, request.categoryId).name,
    description: request.description,
    status: request.status,
    priority: request.priority,
    createdAt: request.createdAt,
    updatedAt: request.updatedAt,
    resolvedAt: request.resolvedAt,
    assigned: Boolean(request.assignedStaffId),
    overdue: isOverdue(db, request),
    version: request.version
  };
  if (isStaff) {
    dto.assignedStaffName = owner ? owner.name : null;
    dto.assignedAt = request.assignedAt;
    dto.requesterReference = `R-${pad(request.requesterId, 4)}`;
    dto.allowedTransitions = [...TRANSITIONS[request.status]];
  }
  if (withHistory) {
    dto.history = request.history
      .filter((entry) => isStaff || entry.previousStatus !== entry.newStatus)
      .map((entry): HistoryEntry => {
        const item: HistoryEntry = {
          id: entry.id,
          previousStatus: entry.previousStatus,
          newStatus: entry.newStatus,
          changedAt: entry.changedAt,
          comment: entry.comment
        };
        if (isStaff) {
          item.changedByName =
            entry.previousStatus === null
              ? `Requester R-${pad(request.requesterId, 4)}`
              : db.users.find((candidate) => candidate.id === entry.changedById)?.name;
        }
        return item;
      });
  }
  return dto;
};

const findStaffRequest = (db: MockDb, user: MockUser, id: string): MockRequest => {
  requireRole(user, 'STAFF');
  const request = db.requests.find((item) => item.id === Number(id)) ?? fail(404, 'We could not find that request.');
  if (!inScope(user, request)) fail(403, 'This request is outside the categories you are authorised to see.');
  return request;
};

const checkVersion = (request: MockRequest, version: unknown): void => {
  if (version !== undefined && version !== null && Number(version) !== request.version) {
    fail(409, 'Someone else changed this request while you were working on it. Reload to see the latest version, then try again.');
  }
};

const record = (db: MockDb, request: MockRequest, user: MockUser, previousStatus: Status, newStatus: Status, comment: string | null): string => {
  const at = new Date().toISOString();
  request.history.push({
    id: db.seq.history++,
    previousStatus,
    newStatus,
    changedById: user.id,
    changedAt: at,
    comment: comment || null
  });
  request.updatedAt = at;
  request.version += 1;
  return at;
};

const routes: Route[] = [];

const openRoute = (method: string, pattern: RegExp, run: (input: RouteInput) => unknown): void => {
  routes.push({ method, pattern, run });
};

const securedRoute = (method: string, pattern: RegExp, run: (input: SecuredInput) => unknown): void => {
  routes.push({ method, pattern, run: (input) => run({ ...input, user: authenticate(input.db, input.token) }) });
};

openRoute('POST', /^\/auth\/login$/, ({ db, body }) => {
  const email = String(body.email || '').trim().toLowerCase();
  const user = db.users.find((item) => item.email === email && item.password === body.password) ?? fail(401, 'Email or password is incorrect.');
  return { token: `mock.${user.id}`, user: publicUser(user) };
});

openRoute('POST', /^\/auth\/register$/, ({ db, body }) => {
  const name = String(body.name || '').trim();
  const email = String(body.email || '').trim().toLowerCase();
  const password = String(body.password || '');
  if (!name) fail(400, 'Enter your name.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) fail(400, 'Enter a valid email address.');
  if (password.length < 8) fail(400, 'Use a password with at least 8 characters.');
  if (db.users.some((item) => item.email === email)) fail(409, 'An account with this email already exists.');
  const user: MockUser = { id: db.seq.user++, name, email, password, role: 'REQUESTER', categoryIds: [] };
  db.users.push(user);
  return { token: `mock.${user.id}`, user: publicUser(user) };
});

securedRoute('GET', /^\/users\/me$/, ({ user }) => publicUser(user));

securedRoute('GET', /^\/categories$/, ({ db }) => db.categories.map(({ id, name }) => ({ id, name })));

securedRoute('POST', /^\/requests$/, ({ db, user, body }) => {
  requireRole(user, 'REQUESTER');
  const category = db.categories.find((item) => item.id === Number(body.categoryId)) ?? fail(400, 'Choose a category for this request.');
  const description = String(body.description || '').trim();
  if (description.length < 10) fail(400, 'Describe the problem in at least 10 characters.');
  const id = db.seq.request++;
  const at = new Date().toISOString();
  const request: MockRequest = {
    id,
    referenceNumber: `CC-${new Date().getFullYear()}-${pad(id)}`,
    categoryId: category.id,
    description,
    status: 'SUBMITTED',
    priority: isPriority(body.priority) ? body.priority : 'NORMAL',
    requesterId: user.id,
    assignedStaffId: null,
    assignedAt: null,
    createdAt: at,
    updatedAt: at,
    resolvedAt: null,
    version: 1,
    history: [{ id: db.seq.history++, previousStatus: null, newStatus: 'SUBMITTED', changedById: user.id, changedAt: at, comment: null }]
  };
  db.requests.push(request);
  return toDto(db, request, user, true);
});

securedRoute('GET', /^\/requests\/mine$/, ({ db, user }) => {
  requireRole(user, 'REQUESTER');
  return db.requests
    .filter((item) => item.requesterId === user.id)
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
    .map((item) => toDto(db, item, user));
});

securedRoute('GET', /^\/requests$/, ({ db, user, params }) => {
  requireRole(user, 'STAFF');
  let list = db.requests.filter((item) => inScope(user, item));
  if (params.categoryId) list = list.filter((item) => item.categoryId === Number(params.categoryId));
  if (params.status) list = list.filter((item) => item.status === params.status);
  if (params.search) {
    const term = params.search.toLowerCase();
    list = list.filter((item) => item.referenceNumber.toLowerCase().includes(term) || item.description.toLowerCase().includes(term));
  }
  const direction = params.sort === 'age_asc' ? -1 : 1;
  list.sort((a, b) => direction * (Date.parse(a.createdAt) - Date.parse(b.createdAt)));
  return list.map((item) => toDto(db, item, user));
});

securedRoute('GET', /^\/requests\/(\d+)$/, ({ db, user, args }) => {
  if (user.role === 'MANAGER') fail(403, 'You do not have permission to do that.');
  if (user.role === 'REQUESTER') {
    const own = db.requests.find((item) => item.id === Number(args[0]) && item.requesterId === user.id) ?? fail(404, 'We could not find that request.');
    return toDto(db, own, user, true);
  }
  return toDto(db, findStaffRequest(db, user, args[0]), user, true);
});

securedRoute('POST', /^\/requests\/(\d+)\/assign$/, ({ db, user, args, body }) => {
  const request = findStaffRequest(db, user, args[0]);
  checkVersion(request, body.version);
  if (request.status !== 'SUBMITTED') fail(400, 'Only a submitted request can be accepted.');
  const at = record(db, request, user, 'SUBMITTED', 'ASSIGNED', null);
  request.status = 'ASSIGNED';
  request.assignedStaffId = user.id;
  request.assignedAt = at;
  return toDto(db, request, user, true);
});

securedRoute('POST', /^\/requests\/(\d+)\/status$/, ({ db, user, args, body }) => {
  const request = findStaffRequest(db, user, args[0]);
  checkVersion(request, body.version);
  const target = body.targetStatus as Status;
  if (!TRANSITIONS[request.status].includes(target)) {
    fail(400, `A request that is ${STATUS_LABELS[request.status].toLowerCase()} cannot move to ${(STATUS_LABELS[target] ?? String(target)).toLowerCase()}.`);
  }
  if (target === 'ASSIGNED') fail(400, 'Accept the request to assign it.');
  const comment = String(body.comment || '').trim();
  if (target === 'RESOLVED' && !comment) fail(400, 'Add a resolution note before resolving this request.');
  const at = record(db, request, user, request.status, target, comment);
  request.status = target;
  if (target === 'RESOLVED') request.resolvedAt = at;
  return toDto(db, request, user, true);
});

securedRoute('POST', /^\/requests\/(\d+)\/comments$/, ({ db, user, args, body }) => {
  const request = findStaffRequest(db, user, args[0]);
  checkVersion(request, body.version);
  if (request.status === 'RESOLVED' || request.status === 'CLOSED') {
    fail(400, 'This request is read-only because it has been resolved.');
  }
  const comment = String(body.comment || '').trim();
  if (!comment) fail(400, 'Write a note before adding it.');
  if (comment.length > 1000) fail(400, 'Keep notes under 1000 characters.');
  record(db, request, user, request.status, request.status, comment);
  return toDto(db, request, user, true);
});

securedRoute('GET', /^\/reports\/summary$/, ({ db, user, params }): Report => {
  requireRole(user, 'MANAGER');
  const from = params.from ? Date.parse(`${params.from}T00:00:00+02:00`) : 0;
  const to = params.to ? Date.parse(`${params.to}T23:59:59.999+02:00`) : Date.now();
  const inRange = db.requests.filter((item) => {
    const created = Date.parse(item.createdAt);
    const sameCategory = !params.categoryId || item.categoryId === Number(params.categoryId);
    return created >= from && created <= to && sameCategory;
  });
  const totals: Record<Status, number> = { SUBMITTED: 0, ASSIGNED: 0, IN_PROGRESS: 0, RESOLVED: 0, CLOSED: 0, REJECTED: 0 };
  inRange.forEach((item) => {
    totals[item.status] += 1;
  });
  const byCategory = db.categories
    .filter((category) => !params.categoryId || category.id === Number(params.categoryId))
    .map((category) => {
      const items = inRange.filter((item) => item.categoryId === category.id);
      const finished = items.filter((item) => item.resolvedAt);
      const average = finished.length
        ? finished.reduce((sum, item) => sum + hoursBetween(item.createdAt, item.resolvedAt ?? item.createdAt), 0) / finished.length
        : null;
      return {
        categoryId: category.id,
        categoryName: category.name,
        total: items.length,
        open: items.filter((item) => OPEN_STATUSES.includes(item.status)).length,
        overdue: items.filter((item) => isOverdue(db, item)).length,
        resolved: items.filter((item) => item.status === 'RESOLVED').length,
        closed: items.filter((item) => item.status === 'CLOSED').length,
        avgResolutionHours: average
      };
    })
    .filter((category) => category.total > 0);
  return {
    total: inRange.length,
    totals,
    openCount: inRange.filter((item) => OPEN_STATUSES.includes(item.status)).length,
    overdueCount: inRange.filter((item) => isOverdue(db, item)).length,
    byCategory,
    requests: inRange
      .slice()
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .map((item) => ({
        id: item.id,
        referenceNumber: item.referenceNumber,
        categoryName: findCategory(db, item.categoryId).name,
        status: item.status,
        createdAt: item.createdAt,
        overdue: isOverdue(db, item)
      }))
  };
});

export const handleMockRequest = async (method: string, path: string, body: Body = {}, params: Params = {}, token?: string): Promise<unknown> => {
  await new Promise((resolve) => setTimeout(resolve, LATENCY_MS));
  const db = loadDb();
  for (const entry of routes) {
    if (entry.method !== method) continue;
    const match = entry.pattern.exec(path);
    if (!match) continue;
    const result = entry.run({ db, token, body, params, args: match.slice(1) });
    saveDb();
    return result;
  }
  return fail(404, 'We could not find what you asked for.');
};
