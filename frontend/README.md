# ITSM frontend

The React frontend uses Google sign-in through Supabase Auth and sends the Supabase access token to Spring. Incident records, assignments, SLA, audit, and notifications come from Spring's API.

## Run locally

1. Start Spring from the repository root with `./mvnw spring-boot:run`. The default database is H2. For shared development data, export the database settings and run `./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase`.
2. In this directory, copy `.env.example` to `.env.local` and set `VITE_SUPABASE_PUBLISHABLE_KEY` to the project's publishable key from the Supabase dashboard. Use the key starting with `sb_publishable_`. The frontend configuration is public.
3. Run `npm ci` and `npm run dev`, then visit `http://localhost:3000`. The Vite proxy forwards `/api` to Spring on port 8080.
4. Add `http://localhost:3000/` to the Supabase Auth redirect allowlist. Link approved Supabase accounts to ITSM employees or technicians before signing in. For an administrator, set the Spring server's `ITSM_ADMIN_USER_IDS` to the approved Supabase UUIDs, separated by commas.

## Build and deploy

Run `npm run lint` and `npm run build`. Serve `dist/` with an SPA fallback to `index.html` and route `/api/` to Spring on the same public origin. Add that origin's sign-in return URL to the Supabase redirect allowlist.

See the [API contract](../docs/incident-api.md) and [database migration order](../docs/audit-notifications-sla.md#schema-changes-for-the-supabase-schema).

### Email and password test accounts

The login page accepts both Google and email/password through Supabase Auth.
For the shared database, apply `database/supabase-auth-passwords.sql` so that
employee and technician records can be created without a legacy password hash.
In Supabase Authentication, enable the Email provider and create confirmed test
users through the dashboard or the server-side Auth Admin API. Use synthetic
addresses and unique passwords for test accounts. Link each Auth user ID to its
employee or technician record in `account_links`, or use the backend admin
allowlist for an administrator. The app checks these permissions after either
sign-in method.
