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
| `GET /api/notifications?email=...` | A user's notifications, newest first |
| `GET /api/notifications/unread-count?email=...` | `{"unread": n}` for the in-app inbox |
| `PATCH /api/notifications/{id}/read` | Marks a notification read |
| `GET /api/incidents/{id}/notifications` | Every notification sent about an incident |
| `GET /api/incidents/{id}/sla` | SLA snapshot: deadlines, states, minutes left |
| `GET /api/sla/breaches` | Incidents with a breached SLA |
| `GET /api/sla/policies` | Targets for P1–P4 |
| `PUT /api/sla/policies/{priority}` | `{"responseMinutes": 30, "resolutionMinutes": 240}`. Applies to new incidents only |
| `POST /api/sla/check` | Runs breach detection now (useful in the demo) |

Incident responses now also include `responseDueAt`, `resolutionDueAt`, `respondedAt`, `resolvedAt`, `responseBreached` and `resolutionBreached`.

The `?email=` parameter is temporary. Once sign-in is integrated, the inbox should use the signed-in user.

## Schema changes (for the Supabase schema)

- `incidents`: add `response_due_at`, `resolution_due_at`, `responded_at`, `resolved_at` (timestamp, nullable), plus `response_breached` and `resolution_breached` (boolean, not null, default false).
- New tables: `audit_logs`, `notifications`, `sla_policies`. See the `AuditLog`, `Notification` and `SlaPolicy` entities.
