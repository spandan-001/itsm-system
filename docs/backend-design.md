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

3. **Automatic categorisation** (`service/classification`): a multinomial Naive Bayes classifier (written from scratch, Laplace smoothing, softmax confidence) suggests a category from the report text. It trains on 360 labelled examples (`src/main/resources/classifier/training-data.tsv`) plus every resolved incident, and retrains each time an incident closes, so it learns from the organisation's own history. **5-fold cross-validated accuracy: 82%** on unseen reports, against 11% for guessing among 9 categories. The report form shows the suggestion live; on creation, a suggestion at least 85% confident replaces a wrong category before duplicate detection and routing, and the audit trail records the correction. `IncidentClassifier` is an interface, so another model (e.g. an LLM) can replace it.

| Method and path | Who | Purpose |
| --- | --- | --- |
| `POST /api/incidents/classify` | any signed-in user | `{"title": "...", "description": "..."}` → category, confidence, runners-up |
| `GET /api/classifier` | admin | Training size, vocabulary, cross-validated accuracy, settings |
| `PUT /api/classifier/settings` | admin | `{"autoCorrect": true, "threshold": 0.85}` |
| `POST /api/classifier/retrain` | admin | Retrain now |

4. **Suggested resolution** (`service/knowledge`): technicians see the fixes recorded for the most similar past incidents. Score = 0.6 × title + 0.4 × description similarity (the duplicate-detection cosine), +0.10 same category, +0.05 same service, +0.05 if the reporter confirmed the fix, +0.03 per rating star above 3. One click copies a fix into the resolution summary. `GET /api/incidents/{id}/suggested-resolutions`.
5. **Automatic escalation** (`service/escalation`): a P1/P2 incident nobody has acknowledged moves up Technician → Team Lead → System Administrator → IT Manager at 1×, 1.5× and 2× its response target. Each step is audited and notifies that level's contact (configurable; simulated by default) and the technician.
6. **Incident correlation / outage detection** (`service/outage`): three or more open reports (duplicates included) against one IT service within 30 minutes open a `ServiceOutage`. The incidents are linked, the system administrator and IT manager are alerted, reporters are told it is a known outage, later reports join it, and it resolves itself when its last incident is resolved. `GET /api/outages`.
7. **Predictive SLA risk** (`SlaPredictionService`): each open incident gets a predicted resolution time = median of similar past resolutions (same category and priority → same priority → same category, at least 3 examples; else half the target), × (1 + 0.25 per other open incident of the technician), or × 1.5 while unassigned. HIGH risk when the prediction is after the deadline. `GET /api/incidents/{id}/sla-prediction`, `GET /api/sla/at-risk`.
8. **Number of affected users** feeds priority: the weighted score is ×1 / ×1.5 / ×2 for <10 / 10-99 / 100+ people; the ITIL matrix moves up one level at 10 and another at 100 (`PriorityFactors`).
9. **SLA early warning**: an "SLA at risk" notice once 75% of a deadline's window has passed.

The admin overview refreshes itself every 15 seconds (polling while the tab is visible, not server push) and shows analytics charts: incidents per day, and by status, priority, category and service.

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

### Investigation, resolution and feedback

| Method and path | Who | Purpose |
| --- | --- | --- |
| `GET /api/incidents/{id}/notes` | anyone who can see the incident | The thread, oldest first. Employees don't see `INVESTIGATION` notes |
| `POST /api/incidents/{id}/notes` | see below | `{"type": "INFO_REQUEST", "body": "Which floor are you on?"}` |
| `PATCH /api/incidents/{id}/status` | role rules below | `{"status": "RESOLVED", "comment": "Rebooted AP-3"}`. `comment` is **required** for `RESOLVED` (the resolution summary), is the reason when reopening, and otherwise becomes a note |
| `POST /api/incidents/{id}/feedback` | the reporter | `{"rating": 4, "comment": "Quick fix"}`, once, after the incident is resolved |

Note types: employees post `ADDITIONAL_INFO` and `INFO_RESPONSE`; technicians and admins post `INVESTIGATION` (internal) and `INFO_REQUEST`. `RESOLUTION`, `REOPEN_REASON` and `STATUS_COMMENT` are written by the system. An `INFO_REQUEST` sets `awaitingReporterInfo` on the incident until the reporter replies.

Status rules by role: employees `VERIFIED`, `CLOSED`, and reopen (`RESOLVED → IN_PROGRESS`); technicians `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`; admins any move the state machine allows. Admins without a linked account get role `ADMIN` from `/api/me` and can open every incident.

New incident fields: `awaitingReporterInfo`, `resolutionSummary`, `resolvedBy`, `closedAt`, `feedbackRating`, `feedbackComment`, `feedbackAt`, `responseWarned`, `resolutionWarned`.
New search filters: `incidentId`, `awaitingInfo`. New analytics: `byService`, `criticalOpenIncidents`, `resolvedIncidents`, `awaitingReporterInfo`, `averageFeedbackRating`.
The SLA monitor also sends an "SLA at risk" warning once 75% of a deadline's window has passed (`POST /api/sla/check` runs it now). Manually assigning a technician who already has 5 open incidents returns 409 (`TechnicianWorkloadExceededException`).

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
itsm.classifier.auto-correct=true
itsm.classifier.auto-correct-threshold=0.85
itsm.escalation.priorities=P1,P2
itsm.escalation.team-lead=Network Team Lead <team.lead@example.test>
itsm.escalation.system-administrator=System Administrator <sysadmin@example.test>
itsm.escalation.it-manager=IT Manager <it.manager@example.test>
itsm.outage.threshold=3
itsm.outage.window-minutes=30
```

## Demo data

On an empty database, `DemoDataSeeder` creates 5 services, 3 employees and 4 technicians (Kavya, the authentication specialist, is unavailable), plus 8 incidents through the real workflow:

- a closed Wi-Fi incident with a 5-star fix (its fix is suggested for the open Wi-Fi incident);
- an open Wi-Fi incident, a duplicate of it, and a third Wi-Fi report: together they open a suspected **Campus Wi-Fi outage**;
- a resolved portal incident, a closed printer incident, a queued email incident (nobody free), and an assigned database incident.

Demo ideas:
- Show the queued email incident, then `PATCH /api/technicians/4/availability {"available": true}`; it is routed to Kavya immediately and the audit trail shows it.
- Open the Wi-Fi incident as a technician: the suggested fix, the outage banner and the predicted resolution time are all there.
- `POST /api/sla/check` after the P1's response target passes shows escalation and breach notifications.

## For the database and sign-in work

- `database/backend-additions.sql` adds the new columns and tables to the Supabase schema. It must run before `ddl-auto=validate` will pass.
- Tests use the `test` profile (`src/test/resources/application-test.properties`), which forces an in-memory H2 database, so `./mvnw test` never touches Supabase.
- Audit entries record the signed-in user's linked ITSM email (`CurrentActorProvider`).
- Run sections 8–13 of `database/backend-additions.sql` on Supabase for notes, feedback, classification, escalation, outages and affected users.
