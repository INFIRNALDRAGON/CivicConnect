# CivicConnect frontend

React 18, TypeScript, Tailwind CSS 4 and Vite single-page app for the CivicConnect service request platform (SEN381, Team "Git Gud"). It talks to the Spring Boot REST API described in PED v2.0 (ADR-M2-01, ADR-M2-02).

## Run it

Prerequisites: Node.js 20 or newer.

```
npm install
npm run dev
```

`npm run dev` starts on http://localhost:5173 in mock mode, so no backend is needed. Sign in with a demo account from the login page (password `Passw0rd!`).

To use the real backend, create `.env.development.local` (untracked) with:

```
VITE_USE_MOCK=false
```

The dev server proxies `/api` to `http://localhost:8080` (change with `VITE_DEV_PROXY_TARGET`), so no CORS setup is needed locally.

`npm run build` type-checks with `tsc` and then writes the production bundle to `dist/` with the mock disabled. If the frontend is served from a different origin to the API, set `VITE_API_BASE_URL` to the API address and allow that origin in the backend (`CORS_ALLOWED_ORIGIN`, PED 23.3, never a wildcard). If `dist/` is bundled inside the Spring Boot JAR, forward unknown non-API paths to `index.html`.

Mock data lives in the browser's localStorage under `cc_mock_db_v1`. Remove that key to reset it.

## Roles and routes

| Role | Routes |
| --- | --- |
| Requester | `/submit`, `/requests`, `/requests/:id` |
| Staff | `/staff/queue`, `/staff/requests/:id` |
| Management | `/management/dashboard` |
| Anyone | `/login`, `/register` |

Route guards live in `components/common/ProtectedRoute.tsx`. They are a convenience only. The backend must enforce role and category scope on every endpoint (FR-006, NFR-003, ASR-02).

## Requirement coverage

| Requirement | Where |
| --- | --- |
| FR-001, FR-002 | `RequestForm`: category required, description at least 10 characters, reference number shown on submit |
| FR-003, FR-004 | requester `RequestDetailPage` and `MyRequestsPage` |
| FR-005 | status message on the requester pages, refreshed every 30 seconds (within the 1 minute criterion) |
| FR-006 | staff queue shows only what the API returns for the staff member |
| FR-007 | `RequestFilterBar`: search, category, status, sort by age |
| FR-008, FR-009 | staff `RequestDetailPage`: full detail, Accept responsibility |
| FR-010 | only the next valid transition is offered. The server's `allowedTransitions` wins over the local map in `utils/statusLabels.ts` |
| FR-011, FR-012 | notes with author and time. Resolution note required. Resolved and closed requests are read-only |
| FR-013 to FR-016 | `DashboardPage`: counts by status for a date range (default 7 days), open and overdue, category filter, average time to resolution, CSV download |
| NFR-005 | large touch targets, plain wording, labelled fields, keyboard and screen reader support |
| NFR-003 | no requester personal data is shown to staff or management, JWT kept in sessionStorage, nothing is logged |
| ASR-03 | the request `version` is sent with every change. A 409 shows a "Load latest" prompt |

All times are shown in SAST (Africa/Johannesburg). The API is expected to send UTC ISO timestamps.

## API contract the frontend expects

```
POST /api/auth/login            {email, password}              -> {token, user:{id,name,email,role}}
POST /api/auth/register         {name, email, password}        -> {token, user}
GET  /api/users/me                                              -> user
GET  /api/categories                                            -> [{id, name}]
POST /api/requests              {categoryId, priority, description}
GET  /api/requests/mine                                         -> [request]
GET  /api/requests?categoryId=&status=&sort=age_desc|age_asc&search=
GET  /api/requests/{id}                                         -> request with history
POST /api/requests/{id}/assign  {version}
POST /api/requests/{id}/status  {targetStatus, comment, version}
POST /api/requests/{id}/comments {comment, version}
GET  /api/reports/summary?from=YYYY-MM-DD&to=YYYY-MM-DD&categoryId=
```

Errors should return `{ "message": "..." }` with a sensible status: 400 invalid input or transition, 401 unauthenticated, 403 out of scope, 404 not found, 409 version conflict. The message is shown to the user as written.

Request shape: `id, referenceNumber, categoryId, categoryName, description, status, priority (LOW|NORMAL|HIGH), createdAt, updatedAt, resolvedAt, assigned, overdue, version, history[]`. Staff responses also include `assignedStaffName, assignedAt, requesterReference, allowedTransitions`. History entries: `id, previousStatus, newStatus, changedAt, comment`, plus `changedByName` for staff. An entry whose previous and new status match is a note.

Report shape: `total, totals{STATUS: n}, openCount, overdueCount, byCategory[{categoryId, categoryName, total, open, overdue, resolved, closed, avgResolutionHours}], requests[{id, referenceNumber, categoryName, status, createdAt, overdue}]`.

## Structure notes

The layout follows the agreed structure with the `services/` folder removed. Its job is now done by `utils/api.ts` (HTTP client and login storage) and `utils/mockApi.ts` (the demo backend). See `GUIDE.md` for a file-by-file walkthrough.

- `index.html` sits at the project root because Vite requires it there.
- Small shared pieces were added under `components/common` and `components/layout`, plus `utils/roles.ts` and `types.ts`.
- The `REJECTED` status has a label and message but no UI path. It is not in the five-state design of ED-007 and CR-03 is not approved.
