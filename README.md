# CivicConnect

Community service request management platform - built for SEN381 (Software Engineering 381).

CivicConnect replaces the organisation's fragmented email/phone/WhatsApp/spreadsheet process for logging and tracking community service requests (facility faults, maintenance issues, IT support, lost property, and similar) with a single controlled platform. Requesters submit and track requests, staff triage and resolve them, and management gets visibility into open/overdue/resolved work.

This repository contains both the backend (Java/Spring Boot REST API) and the frontend (React) as a single controlled monorepo. See `docs/PED/` for the full Project Engineering Document - requirements, architecture reasoning, risks, and decisions - this README documents the application as it stands, not the engineering rationale behind it.

## Current Implementation Status

**Backend**
- [x] Project bootstrapped with Spring Boot, Maven build configured
- [x] PostgreSQL datasource connected, application context loads successfully
- [ ] Domain entities (User, ServiceRequest, Category, StatusHistory)
- [ ] Repository / service layers
- [ ] Controlled status-transition logic (State pattern - see ADR-ED-007)
- [ ] REST endpoints
- [ ] Spring Security / RBAC
- [ ] Automated tests beyond the default `contextLoads` smoke test

**Frontend**
- [ ] Project bootstrapped
- [ ] Routing / page structure
- [ ] API service layer
- [ ] Auth context
- [ ] Requester / staff / management pages

## Project Structure

```
civicconnect/
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/civicconnect/
│   │   │   │   ├── CivicConnectApplication.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── ServiceRequestController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   └── CategoryController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── ServiceRequestService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── StatusTransitionService.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── ServiceRequestRepository.java
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── CategoryRepository.java
│   │   │   │   │   └── StatusHistoryRepository.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── ServiceRequest.java
│   │   │   │   │   ├── Category.java
│   │   │   │   │   └── StatusHistory.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ServiceRequestDto.java
│   │   │   │   │   └── CreateRequestDto.java
│   │   │   │   ├── status/                  
│   │   │   │   │   ├── RequestState.java
│   │   │   │   │   ├── SubmittedState.java
│   │   │   │   │   ├── AssignedState.java
│   │   │   │   │   ├── InProgressState.java
│   │   │   │   │   ├── ResolvedState.java
│   │   │   │   │   └── ClosedState.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   └── JwtAuthFilter.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   └── InvalidStatusTransitionException.java
│   │   │   │   └── config/
│   │   │   │       └── AppConfig.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── application-dev.properties
│   │   └── test/
│   │       └── java/com/civicconnect/
│   │           ├── service/
│   │           │   └── StatusTransitionServiceTest.java
│   │           └── controller/
│   │               └── ServiceRequestControllerTest.java
│   └── README.md
├── frontend/
│   └── (React app, structure below)
├── docs/
│   └── milestone2_doc/
├── .gitignore
└── README.md

frontend/
├── package.json
├── vite.config.js 
├── public/
│   └── index.html
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── pages/
│   │   ├── requester/
│   │   │   ├── SubmitRequestPage.jsx
│   │   │   ├── MyRequestsPage.jsx
│   │   │   └── RequestDetailPage.jsx
│   │   ├── staff/
│   │   │   ├── RequestQueuePage.jsx
│   │   │   └── RequestDetailPage.jsx
│   │   ├── management/
│   │   │   └── DashboardPage.jsx
│   │   └── auth/
│   │       ├── LoginPage.jsx
│   │       └── RegisterPage.jsx
│   ├── components/
│   │   ├── common/
│   │   │   ├── Button.jsx
│   │   │   ├── StatusBadge.jsx
│   │   │   └── LoadingSpinner.jsx
│   │   ├── requests/
│   │   │   ├── RequestCard.jsx
│   │   │   ├── RequestForm.jsx
│   │   │   ├── RequestFilterBar.jsx
│   │   │   └── StatusHistoryTimeline.jsx
│   │   └── layout/
│   │       ├── Navbar.jsx
│   │       └── Sidebar.jsx
│   ├── services/
│   │   ├── api.js                  
│   │   ├── requestService.js       
│   │   ├── userService.js
│   │   └── authService.js
│   ├── hooks/
│   │   ├── useAuth.js
│   │   └── useRequests.js
│   ├── context/
│   │   └── AuthContext.jsx
│   ├── utils/
│   │   ├── statusLabels.js         
│   │   └── dateFormat.js
│   └── styles/
│       └── index.css 
```

Backend and frontend each have their own README with setup detail specific to that side - this top-level README is the entry point that ties both together.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security |
| Database | PostgreSQL 15+ |
| Frontend | React |
| Build tools | Maven (backend), npm/yarn (frontend) |

Full rationale and alternatives considered: ADR-M2-01 (`docs/decisions/`).

## Getting Started

You'll need both the backend and frontend running for the full application to work.

# CivicConnect Database Setup Guide

This guide explains how to set up the CivicConnect PostgreSQL database, Docker container, Flyway migrations, Spring Boot configuration, and dummy test data from scratch.

---

# Prerequisites

Before starting, install the following:

- Docker Desktop
- Java JDK 21
- Apache Maven (optional if using Maven Wrapper)
- pgAdmin 4
- Git

---

# 1. Add the Required Files

Create a new file in the project root:

```text
.env
```

by copying:

```text
.env.example
```

and replacing the placeholder passwords with your own values.

Example:

```env
DB_ADMIN_PASSWORD=YourAdminPassword
DB_OWNER_PASSWORD=YourOwnerPassword
DB_APP_PASSWORD=YourAppPassword
DB_READONLY_PASSWORD=YourReadonlyPassword
```

---

# 2. Configure Git for Shell Scripts

Windows sometimes converts shell scripts from LF to CRLF line endings, which can cause Docker initialization scripts to fail.

Add the following to `.gitattributes`:

```text
*.sh text eol=lf
```

---

# 3. Start the Database

Open Docker Desktop and wait until:

```text
Engine Running
```

is displayed.

From the repository root, run:

```bash
docker compose up -d
```

During the first startup:

- The PostgreSQL image is downloaded.
- The database container is created.
- The initialization script runs.
- Database roles are created using the passwords stored in `.env`.

---

# 4. Verify the Database is Healthy

Check container status:

```bash
docker compose ps
```

Wait until the database service shows:

```text
healthy
```

Example:

```text
NAME              STATUS
civicconnect-db   Up (healthy)
```

### Troubleshooting

View container logs:

```bash
docker compose logs db
```

Look for messages containing:

```text
ERROR
FATAL
```

### Reset the Database

If you need to recreate the database from scratch:

```bash
docker compose down -v
```

Then:

```bash
docker compose up -d
```

⚠️ This deletes all existing data.

---

# 5. Connect with pgAdmin

Open pgAdmin and register a new server.

### Connection Settings

```text
Host: localhost
Port: 5433
Database: civicconnect
Username: ccadmin
Password: <DB_ADMIN_PASSWORD>
```

Example:

```text
Host: localhost
Port: 5433
Database: civicconnect
Username: ccadmin
Password: ********
```

### Verify Created Roles

Expand:

```text
Login/Group Roles
```

You should see:

```text
civicconnect_owner
civicconnect_app
civicconnect_readonly
```

Note:

Windows PostgreSQL installations commonly run on port:

```text
5432
```

The CivicConnect Docker database runs on:

```text
5433
```

so the two do not conflict.

---

# 6. Configure Spring Boot

Create:

```text
backend/src/main/resources/application-dev.properties
```

Example configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/civicconnect

spring.datasource.username=civicconnect_app
spring.datasource.password=<DB_APP_PASSWORD>

spring.flyway.user=civicconnect_owner
spring.flyway.password=<DB_OWNER_PASSWORD>

spring.jpa.hibernate.ddl-auto=validate
```

---

# 7. Run the Backend

Navigate to:

```text
backend
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
./mvnw spring-boot:run
```

---

# 8. Verify Startup

The log should show Flyway applying migrations:

```text
Migrating schema "public" to version "1 - init schema"
Migrating schema "public" to version "2 - seed categories"
```

and then:

```text
Started BackendApplication
```

---

# 9. Load Dummy Data

In pgAdmin:

1. Open **Query Tool**
2. Connect to the **civicconnect** database as:

```text
ccadmin
```

3. Open:

```text
dev_seed_dummy_data.sql
```

4. Execute the script.

This populates:

- Users
- Categories
- Service Requests
- Status History
- Notifications
- User Category permissions

---

# 14. Re-running the Dummy Data Script

The seed script is designed so that:

```text
Running it again does not create duplicates.
```

The script also contains a commented cleanup section that can be used to remove all development test data if required.

---

Frontend runs at `http://localhost:5173` (or whatever port your dev server uses) and talks to the backend API.

See `frontend/README.md` for full frontend-specific detail.

## API

The frontend consumes the backend as a REST API. Endpoints are documented in `backend/README.md` and updated as they're implemented - none are built yet, so there's nothing to list here.

## Environment & Configuration

- Local database credentials live in `backend/src/main/resources/application-dev.properties`, which is gitignored and must never be committed.
- Frontend environment variables (e.g. the backend API base URL) belong in `frontend/.env.local`, also gitignored.
- No passwords, API keys, or connection strings are committed anywhere in this repository - see the Master Project Brief's GitHub Governance standard.

## Design Decisions Worth Knowing

- **Status transitions** (`backend/.../status/`): implemented using the State design pattern so each request status knows its own valid next transitions, rather than scattering validation logic across service methods. See ADR-ED-007.
- Further design-pattern documentation will be added here as each is implemented, per the project's second required design decision.

## Known Limitations / TODOs

- No authentication/authorization implemented yet
- No API endpoints implemented yet - backend domain/repository layers are the current priority
- `ddl-auto=update` is a development convenience only and must be replaced with proper migrations before any real deployment
- Frontend project not yet bootstrapped
- Test coverage is currently limited to the default Spring Boot context-load smoke test

## Related Documentation

- Project Engineering Document (PED): `docs/PED/`
- Requirements Traceability Matrix (RTM): `docs/requirements/`
- Architecture Decision Records: `docs/decisions/`
- Risk Register: `docs/risk/`
- GitHub governance standards: see the SEN381 Master Project Brief

## Team

CivicConnect is built as a 3-person team project for SEN381. See the PED's sign-off page for team member names and roles.
