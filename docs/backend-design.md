# Backend design: OOP principles, patterns and endpoints

This is the map for the report and the demo video: where each requirement lives in the code.

## Incident flow

```
POST /api/incidents
  → validate request (Bean Validation, 400 with field errors)
  → priority            PriorityCalculator → active PriorityStrategy
  → duplicate check     DuplicateDetector  → linked to original, stops here (no SLA, no routing)
  → SLA deadlines       SlaService (from the priority's SlaPolicy)
  → save, publish IncidentCreatedEvent
  → NEW → CLASSIFIED    (State pattern validates every move)
  → route               RoutingService → active AssignmentStrategy
       technician found → ASSIGNED
       nobody suitable  → QUEUED, retried when an incident is resolved, a technician
                          becomes available, or every 60 s (no synchronous retry loop)
```

Every change publishes an `IncidentEvent`. Observers write the audit trail, send notifications and stop SLA clocks.

## Design patterns

| Pattern | Where | What it does |
| --- | --- | --- |
| **State** | `state/IncidentState`, `AbstractIncidentState`, 9 state classes, `IncidentStates` | Each status is an object that knows its legal next statuses and entry rules (e.g. ASSIGNED needs a technician). `Incident.transitionTo()` asks the current state; an illegal move throws `InvalidStateTransitionException`. |
| **Strategy** (priority) | `strategy/priority/*` | `WeightedScorePriorityStrategy` (impact × urgency × criticality) and `ImpactUrgencyMatrixPriorityStrategy` (ITIL matrix). `PriorityCalculator` is the context. |
| **Strategy** (assignment) | `strategy/assignment/*` | `SkillBasedAssignmentStrategy`, `LoadBalancedAssignmentStrategy`, `SmartAssignmentStrategy`. `RoutingService` is the context. |
| **Observer** | `event/*`, `AuditEventListener`, `NotificationEventListener`, `SlaEventListener`, `QueueEventListener` | `IncidentService` publishes events and never calls the observers directly. |
| **Template Method** | `AbstractAssignmentStrategy.rank()`, `AbstractNotificationSender.send()` | The fixed algorithm lives in the base class; subclasses fill in one step. |
| **Repository** | `repository/*` | Spring Data JPA hides persistence; `JpaSpecificationExecutor` for dynamic search. |
| **Registry / Generics** | `strategy/StrategyRegistry<T extends NamedStrategy>` | One generic class switches both strategy families at runtime. |

Strategies can be switched live, which is handy in the demo: `PUT /api/strategies/assignment/load`.

## OOP principles

| Principle | Example |
| --- | --- |
| Encapsulation | `Incident` has no public `setStatus`/`setAssignedTechnician`. Status only changes via `transitionTo()`/`assignTo()`, SLA fields via `recordResponse()` etc. `AuditLog` has no setters (append-only). |
| Abstraction | `IncidentState`, `PriorityStrategy`, `AssignmentStrategy`, `NotificationSender`, `TextSimilarity` interfaces |
| Inheritance | `User` → `Employee`, `Technician`; `AbstractIncidentState` → 9 states; `ItsmException` → custom exceptions; `IncidentEvent` → 5 events |
| Polymorphism | `AuditService.record(IncidentEvent)` handles every event through `describe()`/`getAuditAction()`; `NotificationService` loops over every `NotificationSender` |
| Sealed types + pattern matching | `IncidentEvent` is `sealed`; `NotificationEventListener` uses an exhaustive `switch` over event types |
| Composition / Aggregation | `Incident` *has* a reporter, technician, services, and a duplicate link; `NotificationService` is composed of senders |
| Custom exceptions | `ItsmException` → `InvalidStateTransitionException`, `ResourceNotFoundException`, `TechnicianUnavailableException`; mapped to JSON by `ApiExceptionHandler` |

## Intelligent features

1. **Duplicate detection** (`service/duplicate`): cosine similarity of term frequencies (stop words removed, plurals stemmed). Score = 0.6 × title + 0.4 × description, +0.15 if the same IT service is affected. Compared with open incidents of the same category from the last 7 days. ≥ 0.55 links as duplicate. Reporters of duplicates are notified when the original changes.
2. **Smart routing** (`SmartAssignmentStrategy`): 50 points for a skill match, 30 × spare capacity (max 5 open incidents), 20 × experience (similar incidents resolved before). P1 incidents wait for a skilled technician; others fall back to whoever is free.

## Lifecycle

`NEW → CLASSIFIED → (QUEUED →) ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED`

`RESOLVED → IN_PROGRESS` reopens an incident when verification fails, and restarts the resolution SLA clock. Incident JSON includes `allowedTransitions` and `statusDescription` so the UI can show the right buttons.

## Endpoints added

| Method and path | Purpose |
| --- | --- |
| `GET /api/incidents/search` | Filters: `status` (repeatable), `priority` (repeatable), `category`, `technicianId`, `reporterId`, `serviceId`, `createdFrom`, `createdTo` (yyyy-MM-dd), `keyword`, `slaBreached`, `duplicate`, `unassigned`, plus `page`, `size`, `sort` |
| `GET /api/analytics/summary` | Counts by status/priority/category, open/queued/duplicate/breached totals, SLA compliance %, average resolution time (overall and per priority), incidents per day (7 days), per-technician workload |
| `GET /api/incidents/similar?title=&description=&category=` | "Similar incidents already open" check before submitting |
| `GET /api/incidents/{id}/duplicates` | Incidents linked to this one |
| `POST /api/incidents/{id}/not-duplicate` | Undo a duplicate link; the incident gets its own SLA and is routed |
| `GET /api/incidents/{id}/recommendations` | Ranked technicians with score and reason |
| `POST /api/routing/process-queue` | Route queued incidents now |
| `GET /api/technicians` | Technicians with skills and current workload |
| `PATCH /api/technicians/{id}/availability` | `{"available": true}`; routes queued work when someone returns |
| `PUT /api/technicians/{id}/skills` | `{"skills": ["NETWORK", "SECURITY"]}` |
| `GET /api/strategies` | Active and available priority/assignment strategies |
| `PUT /api/strategies/priority/{name}` | `weighted` or `matrix` |
| `PUT /api/strategies/assignment/{name}` | `skill`, `load` or `smart` |

Audit, notification and SLA endpoints are in [audit-notifications-sla.md](audit-notifications-sla.md).

Errors use RFC 9457 problem JSON: `400` validation (with an `errors` map), `404` not found, `409` business-rule conflicts (an invalid transition includes `from`, `to` and `allowedTransitions`).

## Configuration (all optional, defaults shown)

```properties
itsm.priority.strategy=weighted
itsm.routing.strategy=smart
itsm.routing.auto-assign=true
itsm.duplicates.threshold=0.55
itsm.duplicates.window-days=7
itsm.seed.demo-data=true
itsm.sla.check-interval-ms=60000
itsm.routing.queue-interval-ms=60000
```

## Demo data

On an empty database, `DemoDataSeeder` creates 5 services, 3 employees and 4 technicians (Kavya, the authentication specialist, is unavailable), plus 6 incidents through the real workflow: one in progress, one detected duplicate, one resolved, one closed, one queued, one assigned.

Demo idea: show the queued email incident, then `PATCH /api/technicians/4/availability {"available": true}`; it is routed to Kavya immediately and the audit trail shows it.

## For the database and sign-in work

- `database/backend-additions.sql` adds the new columns and tables to the Supabase schema. It must run before `ddl-auto=validate` will pass.
- Tests use the `test` profile (`src/test/resources/application-test.properties`), which forces an in-memory H2 database, so `./mvnw test` never touches Supabase.
- Once Google sign-in works, change `CurrentActorProvider.currentActor()` to return the signed-in user's email; every audit entry then records who did it.
