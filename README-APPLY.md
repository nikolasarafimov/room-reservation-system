# Step 5.3 - Spring Security integration (local development)

This patch was adapted to the original room-reservation-system.zip. Files represent additions/replacements relative to the original archive. The original project was later modified in Step 5.1, so preserve your existing `application.properties` and `docker-compose.yml`; those files are not included here.

## Install

1. Back up the project and the database first. Ensure `.env` is not tracked by Git.
2. Unzip into the project root, preserving `backend/` and `frontend/` directory structure. Review changes before overwriting files; this is not an automatic merge with changes you made since original ZIP.
3. Append the lines in `SECURITY_PROPERTIES_TO_APPEND.txt` to `backend/src/main/resources/application.properties`.
4. Run `./setup-demo-passwords.ps1` from Windows PowerShell in the project root (choose separate strong local passwords).
5. Rebuild via `docker compose up -d --build`. Watch logs.
6. Test at `http://localhost:3000`. Password login is required; no user selection.

## Runtime model

- Spring Security form-login: POST `/api/auth/login` using application/x-www-form-urlencoded `username` + `password`.
- `GET /api/auth/csrf` returns token, stored in server HTTP session. Include header `X-CSRF-TOKEN` on POST/PUT/DELETE and call CSRF endpoint again after login/logout.
- `GET /api/auth/me` returns authenticated user.
- `POST /api/auth/logout` invalidates session.
- `ROLE_ADMIN` may access `/api/users`; `ROLE_APPROVER` and `ROLE_ADMIN` may access pending and approvals.
- Reservation owner is inferred from authenticated user, and approval actor inferred from authenticated user; client supplied IDs are not trusted.
- Reservation details available only to owner or APPROVER/ADMIN.

## Limitations

- No production-ready controls for rate limiting, account recovery, audit trails, full authorization/validation tests, TLS settings, or safe initial user enrollment.
- `RegisterScreen` is still demo-only; it does not create users.
- Ensure session cookie requires Secure when deploying behind HTTPS.
- Password setup script is intended for LOCAL DEMO only and uses temporary plaintext in PowerShell process memory and stdin (no file or command-line argument persistence). Use different passwords for each user.
- The patch was prepared from archive sources but has NOT been compiled or integration-tested in the user's Docker environment.
- For Vite dev-server port 5173, the fetch setup uses same-origin cookies and a same-origin `/api` proxy; the Docker/Nginx configuration on port 3000 is the supported mode for this patch.
