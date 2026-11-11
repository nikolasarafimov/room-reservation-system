# Room Reservation System — Java/JDBC Prototype

This directory contains the original command-line prototype of the **Room Reservation System**. It demonstrates the database-oriented implementation developed before the Spring Boot and React application in the repository root.

The prototype is kept as a separate, historical development stage. It uses its **own PostgreSQL database** and does not depend on the final application's schema initialization scripts.

## Technology

- Java and Maven
- JDBC with prepared statements
- HikariCP connection pooling
- PostgreSQL
- SQL transactions with commit and rollback

## Implemented use cases

| Use case | Description |
| --- | --- |
| **UC0001 — Search available rooms** | Search by reservation date, time interval, capacity, room type, and optional equipment. |
| **UC0002 — Create reservation request** | Select a requester, room and/or equipment, then create a pending reservation transactionally. |
| **UC0003 — Approve or reject reservation** | Select a pending reservation and an approver, then store the decision transactionally. |

These flows have been exercised manually against the isolated prototype database. This prototype has **no login or authenticated role enforcement**: requester and approver accounts are selected from the console. The final Spring Boot application adds server-side authentication and authorization.

## Directory structure

```text
prototype/
├── pom.xml
├── src/
│   └── main/java/mk/finki/roomreservation/
│       ├── App.java
│       └── DatabasePool.java
└── sql/
    ├── schema_creation.sql
    └── data_load.sql
```

## Requirements

- Java JDK compatible with `prototype/pom.xml` (Java 17 or newer for the Java language features used)
- Maven installed, or Maven support in IntelliJ IDEA
- Docker Desktop and PowerShell for the setup below
- A **local**, disposable PostgreSQL database named `room_reservation_prototype`

The instructions assume you are running PowerShell from the **repository root** and have configured the root `.env` file as explained in the [main README](../README.md).

## Set up the isolated database

Start the PostgreSQL service used by the main Docker Compose configuration:

```powershell
docker compose up -d db
docker compose ps
```

Check whether the prototype database already exists:

```powershell
docker compose exec -T db psql -U roomreservation -d postgres -Atc "SELECT datname FROM pg_database WHERE datname = 'room_reservation_prototype';"
```

**Only if that command returns no database name**, create a fresh database:

```powershell
docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U roomreservation -d postgres -c "CREATE DATABASE room_reservation_prototype OWNER roomreservation;"
```

For a **new, empty prototype database only**, copy and execute the scripts in this order:

```powershell
docker cp .\prototype\sql\schema_creation.sql room-reservation-db:/tmp/prototype_schema.sql
docker cp .\prototype\sql\data_load.sql room-reservation-db:/tmp/prototype_data.sql

docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U roomreservation -d room_reservation_prototype -f /tmp/prototype_schema.sql
if ($LASTEXITCODE -ne 0) { throw 'Prototype schema initialization failed.' }

docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U roomreservation -d room_reservation_prototype -f /tmp/prototype_data.sql
if ($LASTEXITCODE -ne 0) { throw 'Prototype data initialization failed.' }
```

**Data-loss warning:** `schema_creation.sql` drops and recreates the `project` schema; `data_load.sql` truncates and reloads all eight tables. Both scripts check the database name, but they **will replace data** in the named prototype database. Do not run them again if you want to preserve existing prototype reservations. Never run them against the final `room_reservation` database.

Verify the initial data:

```powershell
docker compose exec -T db psql -U roomreservation -d room_reservation_prototype -c "SELECT 'buildings' AS table_name, COUNT(*) AS records FROM project.buildings UNION ALL SELECT 'rooms', COUNT(*) FROM project.rooms UNION ALL SELECT 'equipment', COUNT(*) FROM project.equipment UNION ALL SELECT 'room_equipment', COUNT(*) FROM project.room_equipment UNION ALL SELECT 'users', COUNT(*) FROM project.users UNION ALL SELECT 'reservations', COUNT(*) FROM project.reservations UNION ALL SELECT 'reservation_equipment', COUNT(*) FROM project.reservation_equipment UNION ALL SELECT 'approvals', COUNT(*) FROM project.approvals;"
```

A fresh initialization contains **8 tables and 51 demo rows** in total.

## Build and run

Compile from the repository root:

```powershell
.\backend\mvnw.cmd -f .\prototype\pom.xml clean package
```

You can also import `prototype/pom.xml` as a Maven project in IntelliJ IDEA and run the `mk.finki.roomreservation.App` main class.

When prompted, use:

| Setting | Value |
| --- | --- |
| Database URL | `jdbc:postgresql://localhost:5433/room_reservation_prototype` |
| Database username | `roomreservation` |
| Database password | Your **local** PostgreSQL password from `.env` |

`App.java` also accepts `DB_URL`, `DB_USER`, and `DB_PASSWORD` environment variables. Do not commit passwords, IDE run configurations containing passwords, or local environment files.

**Console limitation:** If `System.console()` is unavailable (for example, in some IDE consoles), the password prompt may echo what you type. Prefer a securely configured local environment variable for that situation, and never reuse an institutional or production password.

## Example workflow

1. Start the application and confirm successful database connectivity.
2. Select **1** to search for rooms that are free for a chosen date and time.
3. Select **2** to create a pending reservation for a demo requester.
4. Select **3** to approve or reject the newly created request.
5. Confirm the changed reservation status and corresponding approval row in PostgreSQL.

## Limitations

- Console-only proof of concept; no web interface.
- No user login, authenticated identity, or server-side role authorization.
- Uses sample users and data, not real institutional information.
- Prototype SQL is intended for a disposable database and is not a migration system.
- GitHub Actions automatically builds and packages the prototype, but interactive database use cases are verified manually rather than through automated integration tests.
- Not intended for production deployment.

## Full-stack version

For the maintained Spring Boot, React, and PostgreSQL application, see the [main project README](../README.md).