# Audit trail, notifications and SLA

## How it fits together (Observer pattern)

`IncidentService` publishes an `IncidentEvent` (package `com.itsm.event`) after every change:

| Event | Published when |
| --- | --- |
| `IncidentCreatedEvent` | `createIncident` saves a new incident |
| `IncidentAssignedEvent` | `assignTechnician` moves it to `ASSIGNED` |
| `IncidentStatusChangedEvent` | `updateStatus` makes any other transition |
| `SlaBreachedEvent` | a response or resolution deadline is missed |

Three independent observers react. None of them calls another:

1. `AuditEventListener` writes one `AuditLog` row per event, inside the same transaction, so a rolled-back change leaves no audit entry.
2. `SlaEventListener` stops the response clock at `ACKNOWLEDGED` and the resolution clock at `RESOLVED`. A late stop publishes a `SlaBreachedEvent`.
3. `NotificationEventListener` runs **after commit** and sends each message over every `NotificationSender` (in-app inbox + simulated email written to the log).

`SlaMonitor` runs every 60 s (`itsm.sla.check-interval-ms`) and flags open incidents whose deadlines have passed. Each breach is reported once.

## SLA targets (defaults, seeded into `sla_policies` on startup)

| Priority | Response | Resolution |
| --- | --- | --- |
| P1 | 15 min | 4 h |
| P2 | 1 h | 8 h |
| P3 | 4 h | 24 h |
| P4 | 8 h | 72 h |

SLA states: `ON_TRACK`, `AT_RISK` (75% of the window used), `BREACHED`, `MET`, `NOT_APPLICABLE`.

## Endpoints

| Method and path | Returns |
| --- | --- |
| `GET /api/incidents/{id}/audit` | Chronological audit trail for an incident |
| `GET /api/audit?page=0&size=50` | All audit entries, newest first (paged) |
| `GET /api/notifications` | The signed-in account's notifications, newest first |
| `GET /api/notifications/unread-count` | `{"unread": n}` for the signed-in account's in-app inbox |
| `PATCH /api/notifications/{id}/read` | Marks a notification read |
| `GET /api/incidents/{id}/notifications` | The signed-in account's notifications for an incident it can view |
| `GET /api/incidents/{id}/sla` | SLA snapshot: deadlines, states, minutes left |
| `GET /api/sla/breaches` | Incidents with a breached SLA |
| `GET /api/sla/policies` | Targets for P1–P4 |
| `PUT /api/sla/policies/{priority}` | `{"responseMinutes": 30, "resolutionMinutes": 240}`. Applies to new incidents only |
| `POST /api/sla/check` | Runs breach detection now (useful in the demo) |

Incident responses now also include `responseDueAt`, `resolutionDueAt`, `respondedAt`, `resolvedAt`, `responseBreached` and `resolutionBreached`.

Send a Supabase bearer token. The inbox uses the email from the linked ITSM
account. Audit rows record the signed-in user's linked ITSM email (an
administrator's sign-in email) or `system` for background actions. Global audit, SLA breach lists, manual SLA checks, and SLA
policy changes require administrative access. See the [API handoff](incident-api.md).

## Schema changes (for the Supabase schema)

For a fresh database, apply `database/schema.sql`, then
`database/backend-additions.sql` and `database/account-links.sql`.
On an existing instance with the original schema, apply only the two
additions in one transaction. Keep Hibernate in validation mode.

- `incidents`: add `response_due_at`, `resolution_due_at`, `responded_at`, `resolved_at` (timestamp, nullable), plus `response_breached` and `resolution_breached` (boolean, not null, default false).
- New tables: `audit_logs`, `notifications`, `sla_policies`, `technician_skills`, and `account_links`.
  These use row-level security with no browser access policies. The UI reads
  these features through Spring's authorized API.
