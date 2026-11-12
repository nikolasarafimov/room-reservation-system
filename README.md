# Room Reservation System

[![Room Reservation CI](https://github.com/nikolasarafimov/room-reservation-system/actions/workflows/ci.yml/badge.svg)](https://github.com/nikolasarafimov/room-reservation-system/actions/workflows/ci.yml)

**[Live Demo](https://room-reservation-demo.vercel.app/)** · **[Source Code](https://github.com/nikolasarafimov/room-reservation-system)** · **[Original Java/JDBC Prototype](prototype/README.md)**

A full-stack platform for managing shared rooms and equipment, from availability searches and reservation requests to approvals and utilization reports. Originally developed as a database-oriented academic project, it has evolved into a deployed React and Spring Boot application backed by PostgreSQL.

**Technology:** Java 21 · Spring Boot 3.3.5 · Spring Security · Spring JDBC · PostgreSQL 16 · React · Vite · Vercel · Render · Neon · Docker Compose · Nginx · JUnit 5 · Mockito · Testcontainers · GitHub Actions

> **Project status:** Publicly accessible portfolio demonstration with sample data and a restricted read-only visitor account. This is not a production-hardened service or a security certification.

## Live Demo

**Application:** https://room-reservation-demo.vercel.app/

The deployed application includes a **Demo Visitor** account for exploring its read-only features.

| Login field | Value |
| --- | --- |
| Username | `demo_viewer` |
| Password | Provided separately by the project maintainer; not stored in the repository |
| Access | Read-only demonstration |

### Demo Visitor permissions

| Feature | Demo Visitor |
| --- | --- |
| Dashboard and summary statistics | Available |
| Room availability search | Available |
| Room utilization and equipment reports | Available |
| Create reservation | Blocked |
| Approve or reject reservation | Blocked |
| Administration | Blocked |

The React interface hides restricted actions and displays a **Read-only demo** indicator. More importantly, Spring Security enforces the restrictions on the server: `demo_viewer` cannot create reservations through `POST /api/reservations`, even by sending a request directly to the API. Approval operations are limited to authorized approver and administrator roles. Room availability search uses a POST request but only reads data.

## Application Preview

### Dashboard

Monitor rooms, equipment types, users, pending reservations, and approved reservations.

![Room Reservation System Dashboard](docs/screenshots/dashboard.png)

### Authentication and Room Availability

| Secure Authentication | Room Availability Search |
|:---:|:---:|
| ![Login](docs/screenshots/login.png) | ![Search Rooms](docs/screenshots/search-rooms.png) |

### Reservation and Approval Workflow

| Create Reservation | Reservation Approvals |
|:---:|:---:|
| ![Create Reservation](docs/screenshots/create-reservation.png) | ![Approvals](docs/screenshots/approvals.png) |

### Analytics and Reporting

Explore room utilization and equipment demand through PostgreSQL-backed reports.

![Room Reservation Reports](docs/screenshots/reports.png)

> The screenshots demonstrate application modules. The public Demo Visitor account cannot perform the reservation creation or approval actions shown in the workflow screenshots.

## Core Features

- **Room availability:** Search by date, time range, capacity, room type, and required equipment while excluding conflicting reservations.
- **Reservation requests:** Request a room and/or equipment with validation and an initial `pending` status.
- **Approval workflow:** Authorized approvers and administrators can approve or reject pending requests using PostgreSQL-backed business logic.
- **Role-based access control:** Separate `regular`, `approver`, and `admin` roles, plus an explicitly restricted `demo_viewer` account.
- **Authenticated ownership checks:** Reservation creation uses the authenticated user's ID rather than trusting a client-supplied ID; reservation details are protected by ownership checks for regular users.
- **Reporting:** Database-driven room utilization and equipment demand reports.
- **Session security:** Spring Security login, BCrypt password hashes, HTTP-only session cookies, and CSRF protection.
- **Reproducible development environment:** PostgreSQL, backend, and Nginx-served frontend orchestrated with Docker Compose.
- **Automated verification:** Authorization, validation, API error-handling, application-context, and PostgreSQL integration tests.

## Technology Stack

| Layer | Technologies |
| --- | --- |
| Frontend | React, Vite, Framer Motion, Lucide React, React Hot Toast |
| Backend | Java 21, Spring Boot 3.3.5, Spring Web, Spring Security, Spring JDBC |
| Database | PostgreSQL 16, SQL functions, views, triggers, indexes |
| Authentication | Spring Security sessions, BCrypt, CSRF tokens |
| Local infrastructure | Docker Compose, Nginx |
| Public demo hosting | Vercel (frontend), Render (backend), Neon (PostgreSQL) |
| Testing and CI | JUnit 5, Mockito, MockMvc, Testcontainers, GitHub Actions |
| Original prototype | Java, JDBC, HikariCP, PostgreSQL, Maven |

## Architecture

### Public Demo Deployment

```text
Browser
   |
   v
Vercel — React / Vite frontend
   |  Same-origin /api/* rewrite
   v
Render — Spring Boot REST API + Spring Security
   |  Spring JDBC
   v
Neon — Managed PostgreSQL
   |
   +-- project schema
   +-- tables, views, functions, triggers, and indexes
```

The frontend uses relative `/api` requests. [`frontend/vercel.json`](frontend/vercel.json) forwards those requests to the Render backend, keeping the browser's API requests on the frontend origin. The database connection is configured on the backend through environment variables; credentials must never be embedded in frontend code.

**Backend health endpoint:** https://room-reservation-backend-w5s4.onrender.com/api/health

### Local Docker Deployment

```text
Browser — http://localhost:3000
   |
   v
Nginx — React production build (:80 in container)
   |  /api reverse proxy
   v
Spring Boot + Spring Security (:8080)
   |  JDBC
   v
PostgreSQL 16 (:5432 in container; 127.0.0.1:5433 on host)
```

The backend uses **Spring JDBC**, not an ORM. SQL definitions and sample data are versioned in [`database/`](database/).

## Quick Start — Windows PowerShell and Docker Desktop

**Requirements:** Git, PowerShell, and Docker Desktop with Linux containers enabled. Java and Node.js are not required for the Docker-based launch.

### 1. Clone and configure

```powershell
git clone https://github.com/nikolasarafimov/room-reservation-system.git
cd room-reservation-system
Copy-Item .env.example .env
```

Open `.env` and replace `CHANGE_ME_TO_A_STRONG_LOCAL_PASSWORD` with a strong, unique **local** database password. Do not commit `.env`.

### 2. Start PostgreSQL

```powershell
docker compose up -d db
docker compose ps
```

Wait until the database container is `healthy` before running the initialization scripts.

### 3. Initialize a fresh local database

Run the scripts **once and in order**, against a new, empty `room_reservation` database volume.

```powershell
$files = @('01_schema.sql', '02_seed.sql', '03_advanced.sql', '05_auth_schema.sql')
foreach ($file in $files) {
    docker cp ".\database\$file" "room-reservation-db:/tmp/$file"
    if ($LASTEXITCODE -ne 0) { throw "Copy failed: $file" }

    docker compose exec -T db psql -X -v ON_ERROR_STOP=1 --single-transaction -U roomreservation -d room_reservation -f "/tmp/$file"
    if ($LASTEXITCODE -ne 0) { throw "Database initialization failed: $file" }
}
```

These scripts create the `project` schema, core tables, demo records, reporting and approval SQL objects, and authentication columns. They include database-name checks and are intended for fresh **local** setup, not for repeat execution or managed-database migrations. **Do not run these initialization scripts against the public Neon database.**

### 4. Provision local account passwords

```powershell
.\scripts\provision-demo-passwords.ps1
```

The script prompts for distinct passwords for `student1`, `approver1`, and `admin`. Passwords are converted to BCrypt-compatible hashes in PostgreSQL using `pgcrypto`; plaintext passwords are not committed to the repository.

| Username | Role | Typical access |
| --- | --- | --- |
| `student1` | `regular` | Search and create reservation requests |
| `approver1` | `approver` | Review and decide pending reservations |
| `admin` | `admin` | Administrative data and approvals |

The public `demo_viewer` account is configured separately in the hosted database and is not part of the default local seed or password-provisioning script. Additional seeded users have no password until separately provisioned. The registration screen is a UI demonstration; self-registration is not implemented.

### 5. Build and launch

```powershell
docker compose up -d --build
docker compose ps
```

- **Frontend:** http://localhost:3000
- **Backend:** http://localhost:8080
- **PostgreSQL:** `127.0.0.1:5433`

To inspect logs:

```powershell
docker compose logs --tail=80 backend
docker compose logs --tail=40 frontend
```

To stop the stack while preserving the database volume:

```powershell
docker compose down
```

Avoid `docker compose down -v` unless you intentionally want to delete the local database and all its data.

## API Reference

Endpoints other than public health, CSRF-token retrieval, and login require authentication. State-changing requests require a valid CSRF token.

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `GET` | `/api/health` | Public backend and database health check |
| `GET` | `/api/auth/csrf` | Obtain session CSRF token |
| `POST` | `/api/auth/login` | Session login using form-encoded credentials |
| `GET` | `/api/auth/me` | Current authenticated user |
| `POST` | `/api/auth/logout` | End authenticated session |
| `GET` | `/api/dashboard` | Dashboard statistics |
| `GET` | `/api/rooms` | List rooms |
| `POST` | `/api/rooms/search` | Read-only room availability search |
| `GET` | `/api/equipment` | List equipment |
| `POST` | `/api/reservations` | Create reservation; blocked for `demo_viewer` |
| `GET` | `/api/reservations/{id}` | Reservation details with ownership/role checks |
| `GET` | `/api/reservations/pending` | Pending requests; approver/admin |
| `POST` | `/api/approvals` | Approve or reject; approver/admin |
| `GET` | `/api/approvers` | List approvers; approver/admin |
| `GET` | `/api/reports/room-utilization` | Room utilization report |
| `GET` | `/api/reports/equipment-demand` | Equipment demand report |
| `GET` | `/api/users` | User listing; admin only |

The React application calls these endpoints via the same-origin `/api` path. This is proxied by Vercel in the public deployment and by Nginx in the local Docker environment.

## Database Design

The `project` schema contains eight core tables:

`buildings` · `rooms` · `equipment` · `room_equipment` · `users` · `reservations` · `reservation_equipment` · `approvals`

Important SQL objects include:

- `v_pending_reservation_details` — pending requests and their related information.
- `v_quarterly_room_utilization` — quarterly room-usage reporting.
- `fn_approve_or_reject_reservation` — reservation decision workflow.
- Database triggers for reservation overlap checks and approver validation.
- Indexes that support operational queries and reporting.

See [`database/`](database/) for the schema, sample data, and advanced SQL definitions.

## Automated Tests and CI

[**View GitHub Actions**](https://github.com/nikolasarafimov/room-reservation-system/actions)

The [CI workflow](.github/workflows/ci.yml) runs three independent jobs on pushes, pull requests, and manual dispatch:

1. **Backend — Build and Test:** Java 21, selected Spring Security authorization, reservation validation, API error-handling, Demo Visitor, Spring context, and PostgreSQL Testcontainers integration tests; then application packaging.
2. **Frontend — Build:** Node.js 22, `npm ci`, and a Vite production build.
3. **Prototype — Build Java 17:** Maven package build and executable JAR verification.

The backend test selection includes:

```text
SecurityAuthorizationTests
ReservationAuthorizationTests
ReservationValidationTests
ApiExceptionHandlerTests
PostgreSqlIntegrationTests
DemoViewerAuthorizationTests
BackendApplicationTests
```

Run the full default Maven verification lifecycle locally (Java 21 and Docker required for Testcontainers):

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml clean verify
```

Build the React frontend locally (Node.js and npm required):

```powershell
cd frontend
npm ci
npm run build
```

The authorization tests check that the Demo Visitor cannot create reservations and can still search for rooms. Frontend visibility controls improve the experience but do **not** replace server-side authorization.

## Original Java/JDBC Prototype

The repository preserves the initial command-line version developed before the full-stack application.

It demonstrates three use cases:

- **UC0001 — Search Available Rooms:** Filter rooms by date, time, capacity, type, and equipment.
- **UC0002 — Create Reservation Request:** Create pending requests with JDBC transactions.
- **UC0003 — Approve or Reject Reservation:** Process decisions with transactional database logic.

**Technologies:** Java, JDBC, HikariCP, PostgreSQL, Maven.

The prototype uses a separate `room_reservation_prototype` database, independently of the Spring Boot application.

[**Explore the original prototype**](prototype/README.md)

## Repository Structure

```text
.github/workflows/ci.yml     GitHub Actions workflow
backend/                     Spring Boot REST API, security, and tests
frontend/                    React application, Vercel rewrites, and Nginx
prototype/                   Original Java/JDBC CLI implementation
database/                    PostgreSQL schema, seed data, and advanced SQL
docs/screenshots/            Application screenshots
scripts/                     Local account password provisioning
.env.example                 Local environment template
docker-compose.yml           Local three-service stack
```

## Security and Operational Notes

This project demonstrates session-based authentication, BCrypt password storage, CSRF protection, role-based authorization, and server-side request ownership checks. The `demo_viewer` account is intentionally read-only; its restrictions are enforced by the backend, not merely by hiding UI controls.

The public deployment uses **sample data** and is designed for portfolio evaluation. It should not be treated as a security-audited production service. Further hardening would include rate limiting, credential rotation and recovery procedures, audit logging, monitoring, secure deployment configuration reviews, and ongoing dependency updates.

Never commit `.env` files, database passwords, session secrets, private keys, real user information, or production database exports. Use separate credentials for local development and hosted services.

## Author

Developed by **[Nikola Sarafimov](https://github.com/nikolasarafimov)** as an academic database project and full-stack software engineering portfolio application.