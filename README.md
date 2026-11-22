# Intelligent IT Service Management & Incident Resolution System

A course project for CS F213 (Object-Oriented Programming). The system tracks IT incidents through their whole lifecycle:

- An employee reports a problem.
- The system works out its priority and category, spots duplicates and service outages, and routes it to a suitable technician.
- SLA clocks run, escalations happen if nobody responds, and the reporter confirms the fix and gives feedback.

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1 (Spring MVC, Data JPA, Bean Validation, Security, Actuator) |
| Frontend | React with Vite, in [`frontend/`](frontend/) |
| Database | H2 in memory for local runs and tests; Supabase PostgreSQL for shared data |
| Sign-in | Google through Supabase Auth; Spring checks the Supabase JWT |

## Features

**The incident lifecycle.** Each incident moves NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED, with QUEUED for incidents waiting on a technician. The State pattern rejects any illegal move, and each role may only make its own transitions:

- **Employees** can verify, close or reopen.
- **Technicians** can acknowledge, start work or resolve.
- **Administrators** can make any transition.

**Intelligent features:**

1. **Priority calculation.** Priority (P1–P4) comes from impact, urgency, how critical the affected services are, and how many users are affected. There are two strategies, and you can switch between them while the app runs.
2. **Category classification.** A Naive Bayes classifier, written from scratch, suggests a category and corrects wrong ones it is confident about. It reaches 82% five-fold cross-validated accuracy and retrains whenever an incident closes.
3. **Duplicate detection.** Cosine text similarity links new reports to an open incident that matches.
4. **Smart routing.** Technicians are scored on skill match, spare capacity and experience. If nobody suitable is free, the incident is queued and retried.
5. **Suggested resolutions.** Technicians see how the most similar past incidents were fixed.
6. **Outage detection.** Three or more reports on one service within 30 minutes open a suspected outage, and operations are alerted.
7. **Automatic escalation.** An unacknowledged P1 or P2 incident goes to the Team Lead, then the System Administrator, then the IT Manager.
8. **SLA prediction.** The predicted finish time comes from past resolution times and the technician's workload. Incidents at risk of missing their SLA are flagged before they breach.
9. **SLA monitoring.** Response and resolution deadlines are tracked, with a warning at 75% of the time used, then breach detection, then escalation.

**Other features:**

- **Audit trail:** every change is logged.
- **Notifications:** in-app and simulated email.
- **Incident thread:** notes, requests for more information, and reporter feedback.
- **Search:** filter incidents by several criteria at once.
- **Admin dashboard:** a live view with analytics charts.

## Design

| Pattern | Where |
| --- | --- |
| State | `state/`: one class per status, which knows its legal next statuses and entry rules |
| Strategy | `strategy/priority/`, `strategy/assignment/` and `service/classification/`, swapped through a generic `StrategyRegistry<T>` |
| Observer | A sealed `IncidentEvent` hierarchy. The audit, notification, SLA and queue listeners react to events without `IncidentService` calling them |
| Template Method | `AbstractAssignmentStrategy` and `AbstractNotificationSender` |
| Repository | Spring Data JPA repositories, with `JpaSpecificationExecutor` for search |

[`docs/backend-design.md`](docs/backend-design.md) maps every OOP principle and pattern to the code, and gives the formulas behind each intelligent feature.

## Project layout

```
src/main/java/com/itsm/
  model/        JPA entities (Incident is the core aggregate; User → Employee, Technician)
  enums/        Status, priority, impact, urgency, category, ...
  state/        State pattern for the incident lifecycle
  strategy/     Priority and assignment strategies
  event/        Sealed incident events (Observer)
  service/      Business logic: incidents, routing, SLA, escalation, outages,
                classification, knowledge, notifications, audit, analytics, search
  controller/   REST API under /api
  config/       Security (Supabase JWT), demo data seeder
  exception/    Custom exceptions → RFC 9457 problem responses
src/main/resources/classifier/   Training data for the classifier
frontend/       React app
database/       Supabase schema, migrations, seed data, ER diagram
docs/           Design notes and API reference
```

## Run the shared demo on your laptop

Clone the integration branch, then follow the backend and frontend steps below in two separate terminals.

```bash
git clone --branch codex/itsm-integration https://github.com/spandan-001/itsm-system.git
cd itsm-system
```

Ask the project owner privately for the shared database connection settings, the demo admin UUID, the frontend publishable key, and the demo login for your role. Use the Supabase backend profile for this walkthrough. If the JDBC URL references a CA certificate, point it to the certificate on your laptop.

If your backend settings are in a private root-level `.env.supabase` file, Spring can read its unquoted `KEY=value` entries directly. Include `ITSM_ADMIN_USER_IDS` for admin sign-in. The optional server-side `SUPABASE_SECRET_KEY` enables the admin **Create user** form.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase '-Dspring-boot.run.arguments=--spring.config.additional-location=file:./.env.supabase[.properties]'
```

Once both servers are running, open http://localhost:3000 and follow the [demo recording walkthrough](docs/demo-walkthrough.md). Each laptop runs its own website and shares incident data through Supabase.

## Running it

You need Java 21. The frontend also needs Node 24 or later.

**Backend.** Run these from the repository root:

```bash
./mvnw spring-boot:run
```

This starts on port 8080 with an in-memory H2 database and demo data: employees, technicians, services and eight incidents, including a Wi-Fi outage. To turn the demo data off, set `itsm.seed.demo-data=false`.

To use the shared Supabase database instead, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, then run:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
```

Hibernate runs in validation mode, so the schema must be applied first (see below).

**Frontend.** Run these from `frontend/`:

```bash
cp .env.example .env.local
```

Set `VITE_SUPABASE_PUBLISHABLE_KEY` in `.env.local`, then:

```bash
npm ci
```

```bash
npm run dev
```

Open http://localhost:3000. Vite forwards `/api` to Spring. [`frontend/README.md`](frontend/README.md) covers the sign-in setup: redirect URLs, linking Supabase accounts to employees or technicians, and `ITSM_ADMIN_USER_IDS` for administrators.

**Database.** For a fresh Supabase database, apply these in order:

1. `database/schema.sql`
2. `database/backend-additions.sql`
3. `database/account-links.sql`
4. `database/supabase-auth-passwords.sql`
5. `database/admin-provisioning.sql`

`database/er-diagram.md` describes every table.

## Tests

```bash
./mvnw test
```

There are 98 tests, which run against H2. They cover:

- State transitions
- Both priority strategies
- Routing and duplicate detection
- The classifier, including its accuracy threshold
- SLA warnings, breaches and prediction
- Escalation and outage detection
- Notes and feedback
- The observers
- Controller and security behaviour

For the frontend, run `npm run lint` and `npm run build`. GitHub Actions runs the backend tests and the frontend checks on every pull request.

## Configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `itsm.priority.strategy` | `weighted` | `weighted` or `matrix` |
| `itsm.routing.strategy` | `smart` | `skill`, `load` or `smart` |
| `itsm.duplicates.threshold` / `itsm.duplicates.window-days` | `0.55` / `7` | How similar a report must be to count as a duplicate, and how far back to look |
| `itsm.classifier.auto-correct` | `true` | Let the classifier correct the category |
| `itsm.classifier.auto-correct-threshold` | `0.85` | Confidence the classifier needs before it corrects a category |
| `itsm.outage.threshold` / `itsm.outage.window-minutes` | `3` / `30` | Outage detection |
| `itsm.escalation.team-lead`, `.system-administrator`, `.it-manager` | Simulated contacts | Escalation recipients, written as `Name <email>` |
| `itsm.sla.check-interval-ms` | `60000` | How often the SLA monitor runs |
| `ITSM_ADMIN_USER_IDS` | (empty) | Supabase user IDs that get administrator access, separated by commas |

Strategies and classifier settings can also be changed at runtime through the admin API. See [`docs/incident-api.md`](docs/incident-api.md).

## Documentation

- [Backend design](docs/backend-design.md): patterns, OOP principles, intelligent features and demo ideas
- [API reference](docs/incident-api.md)
- [Audit, notifications and SLA](docs/audit-notifications-sla.md)
- [ER diagram](database/er-diagram.md)

## Hosting the demo

Build the root Dockerfile with the public build arguments `VITE_SUPABASE_URL` and
`VITE_SUPABASE_PUBLISHABLE_KEY`. The image serves React and the Spring API on
the same origin. Set the host's `PORT` or use the default 8080.

In the host's private runtime environment, configure `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `SUPABASE_AUTH_ISSUER`, `ITSM_ADMIN_USER_IDS`, and
`SUPABASE_SECRET_KEY`. Use a PostgreSQL JDBC URL with TLS and ensure any CA
file referenced in the URL exists on the host. The issuer is the Supabase
project URL followed by `/auth/v1`. The secret key enables the admin
dashboard's **Create user** form.

For a fresh database, also apply `database/supabase-auth-passwords.sql` and
`database/admin-provisioning.sql` after the three schema scripts above. Set
the final HTTPS origin in the Supabase Auth site URL and redirect allowlist.
