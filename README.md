# CivicConnect

Community service request platform for SEN381. Requesters submit and track requests, staff work a category-scoped queue, and managers see service activity. One repository, one project: React frontend, Spring Boot API, PostgreSQL with Flyway migrations.

```
civicconnect/
├── backend/            Spring Boot 4.1 / Java 21 / Maven (REST API, JWT login, JPA, Flyway)
├── frontend/           React 18 + TypeScript + Vite + Tailwind
├── db/00_roles.sql     database roles (owner / app / readonly)
├── docker/init/        container first-start script that applies the roles
├── docker-compose.yml  PostgreSQL, and optionally the whole app (profile "full")
├── Dockerfile          builds the React app and the API into one image
├── docs/               Milestone 2 document, test traceability
├── tests/              k6 performance script and test overview
├── scripts/            test-all.sh / test-all.ps1, git hook installer, CI helpers
├── .github/            CI, CodeQL, dependency review, Dependabot, PR template
└── .env.example        copy to .env
```

## Run it

Requirements: Docker, JDK 21, Node 20+ (22 recommended).

```bash
cp .env.example .env            # then edit the values (letters and digits only for DB_* passwords)
docker compose up -d db         # PostgreSQL on localhost:5433, roles created on first start
```

Backend (new terminal):

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Flyway runs the migrations as `civicconnect_owner`, the API then connects as the least-privileged `civicconnect_app`. With `SPRING_PROFILES_ACTIVE=dev` (the default) the demo data is loaded too.

Frontend (new terminal):

```bash
cd frontend
npm install
npm run dev                     # http://localhost:5173, proxies /api to :8080
```

### Everything in containers

```bash
docker compose --profile full up -d --build     # http://localhost:8080
```

One image: the built React app is served by the API, which falls back to `index.html` for client-side routes.

### Demo accounts (dev profile)

All use the password `Test@1234`.

| Role | Email | Sees |
|---|---|---|
| Requester | `req1@civicconnect.test` ... `req5@` | their own requests and notifications |
| Staff | `staff1@civicconnect.test` | IT support, Maintenance |
| Staff | `staff2@civicconnect.test` | Facility fault, Damaged equipment, Security concern |
| Staff | `staff3@civicconnect.test` | Lost property, Other, IT support |
| Manager | `manager@civicconnect.test` | reports and dashboard |

New people can register in the app; registration always creates a REQUESTER. Staff and manager accounts are provisioned in the database by an administrator.

## How the pieces connect

**Frontend to API.** The browser calls `/api/*` with `Authorization: Bearer <token>`. In development Vite proxies `/api` to port 8080, in the container both are the same origin, so CORS only matters if you host them apart (`APP_CORS_ALLOWED_ORIGINS`).

| Endpoint | Who | Purpose |
|---|---|---|
| `POST /api/auth/login`, `/api/auth/register` | public | returns `{ token, user }` |
| `GET /api/users/me`, `GET /api/categories` | signed in | profile, controlled category list |
| `POST /api/requests`, `GET /api/requests/mine` | requester | submit, own list |
| `GET /api/requests?search&status&categoryId&sort` | staff | category-scoped queue |
| `GET /api/requests/{id}` | owner / staff in scope | detail with history (staff also get internal notes) |
| `POST /api/requests/{id}/assign` `{version}` | staff | accept responsibility |
| `POST /api/requests/{id}/status` `{targetStatus, comment, version}` | staff | start, resolve, close, reject |
| `POST /api/requests/{id}/comments` `{comment, version}` | staff | internal note |
| `GET /api/notifications`, `POST /api/notifications/{id}/read` | requester | in-app messages |
| `GET /api/reports/summary?from&to&categoryId` | manager | dashboard data |

Errors are `{ "message": "..." }` with 400 / 401 / 403 / 404 / 409. A 409 means someone else changed the request first (optimistic locking, ASR-03) or the move is not allowed.

**Backend modules** (modular monolith, ADR-M2-02): `auth`, `request`, `assignment`, `status`, `audit`, `notification`, `reporting`.

- Each module talks to storage through a small interface (`RequestStore`, `StatusHistoryStore`, `NotificationStore`, `UserDirectory`, `CategoryCatalog`). The PostgreSQL implementations are the `Jpa*` classes. The earlier in-memory versions now live in `src/test` as test doubles, which is why the business-rule tests need no database.
- Status changes go through the State pattern (`status/`): Submitted → Assigned → In Progress → Resolved → Closed, plus Rejected (CR-03, only from Submitted, reason mandatory). `StatusTransitionService` runs in one transaction; the audit listener writes `status_history` inside it, and the notification listener runs after commit.
- `service_requests` has no `created_at`: submission, assignment and resolution times come from `status_history` through the `v_request_overview` view (extended in V3).
- Staff notes (FR-011) cannot live in `status_history`, whose rows must change the status, so V3 adds the append-only `request_comments` table.
- Requesters never see staff names, internal notes or other people's requests; reports contain counts and references only.

**Database.** `V1` schema, `V2` categories, `V3` notes table and view update. `db/dev/R__dev_seed.sql` is a repeatable migration loaded only under the `dev` profile.

## Tests

Everything is automated. After you push to GitHub, `.github/workflows/ci.yml` runs on every push and pull request and again every night, so nobody has to remember to run anything. It builds and tests the backend against a real PostgreSQL 15, builds and tests the frontend, and then drives the whole running system with a browser. CodeQL and dependency review run on pull requests.

On your own machine, one command does the same (it uses Docker for a throwaway database and never touches your real one):

```bash
scripts/test-all.sh            # Windows PowerShell: scripts\test-all.ps1
scripts/install-git-hooks.sh   # optional: run the fast checks before every git push
```

Or run the layers separately:

```bash
cd backend && ./mvnw test      # unit and architecture tests, no database needed
cd backend && ./mvnw verify    # plus API tests on PostgreSQL (docker compose up -d db first) and a JaCoCo report
cd frontend && npm test        # unit tests
cd frontend && npm run test:e2e   # browser tests on the built-in mock API
```

`tests/README.md` explains the layout and `docs/TEST_TRACEABILITY.md` maps every test to the PED test IDs (T-, A-, E-, P-) and requirements.

## Before this goes anywhere public

- The role passwords are no longer in `db/00_roles.sql` (placeholders `CHANGE_ME_*`, replaced from `.env`). The earlier version had real-looking passwords in a public repository: treat those as compromised and rotate them wherever they were used.
- Set a strong `APP_AUTH_SECRET`; the API refuses to start without one of at least 32 characters.
- Run with `SPRING_PROFILES_ACTIVE=prod` (and rebuild the frontend with `SHOW_DEMO_ACCOUNTS=false`) so the demo users do not exist.
- Overdue thresholds in V2 are placeholders (24 to 168 hours) until agreed with stakeholders (CR-02).
- If you already have a local database from the old setup, run `docker compose down -v` first: V2 no longer inserts placeholder users, which changes its checksum.
