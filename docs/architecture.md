# Architecture

## Boundaries

React pages call the Axios client through `/api`; a Vite or Nginx same-origin proxy targets Spring Boot. JWT authentication establishes identity from the database on every request. `@PreAuthorize` gates administrative endpoints. `AccessService` enforces incident ownership and assignment inside transactional services, including reads, notes, state operations, feedback and duplicate review. Frontend route guards are usability controls in addition to backend authorization.

`IncidentController` and the other REST controllers accept validated request records and return view records. `ViewMapper` excludes passwords and signing configuration. Entity persistence uses Spring Data repository interfaces; `IncidentChatService`, `IncidentEmailService` and `SmtpOutboxService` use parameterized `JdbcTemplate` SQL for chat and durable email delivery. Controllers contain no SQL. `ReportController` and catalog/user listings use read repositories, while business mutations go through services.

## Business services

- `IncidentService`: create/update and lifecycle orchestration, notes, feedback and duplicate decisions.
- `IncidentQueryService`: scoped reads, combined specifications, pagination and assembled details.
- `PriorityCalculationService`: strategy lookup, priority score/explanation and SLA selection.
- `AssignmentService`: persisted candidate inputs, configured strategy, availability/capacity enforcement and assignment history.
- `DuplicateDetectionService`: Jaccard token similarity, service/category score and persisted suggestions.
- `IncidentChatService`: participant-scoped persisted conversation threads and notification replies.
- `IncidentEmailService` + `EmailScheduler` + `SmtpOutboxService`: transactional email queuing, recipient overrides, expiry checks, retry and real SMTP delivery.
- `SlaMonitoringService` + `SlaScheduler`: persisted warning/breach changes and escalations every 30 seconds.
- `AuthService`, `UserManagementService`, `CatalogService`, `ConfigurationService`, `NotificationService`, `ReportService`: distinct application responsibilities.

## Transaction and concurrency policy

An incident mutation commits its audit entries, in-app notifications, and email outbox entries together. A separate worker delivers queued email via SMTP after commit. `IncidentEventPublisher` synchronously invokes observers; it does not fire uncommitted work in another thread. An observer failure rolls back the mutation and its effects.

Incident operations use a pessimistic incident lock. Assignment also locks the selected technician before recounting workload, so simultaneous assignments cannot silently exceed capacity for that technician. A JPA `@Version` field guards overlapping changes by other writers, including scheduler updates. A conflict produces an HTTP 409 and a reload prompt. A single backend scheduler instance is the demonstrated topology.

Automatic creation passes through NEW and CLASSIFIED before ASSIGNED. If no technician is eligible, the report remains CLASSIFIED and an administrator can assign later. Reassignment preserves investigation progress and writes a separate Assignment row. The current technician is stored on Incident; all historical assignments remain intact.

## Lifecycle

| Current state | Allowed successor |
| --- | --- |
| NEW | CLASSIFIED |
| CLASSIFIED | ASSIGNED |
| ASSIGNED | ACKNOWLEDGED |
| ACKNOWLEDGED | IN_PROGRESS |
| IN_PROGRESS | RESOLVED |
| RESOLVED | VERIFIED or IN_PROGRESS |
| VERIFIED | CLOSED or IN_PROGRESS |
| CLOSED | IN_PROGRESS |

Eight concrete states extend a shared state validator. `Incident.transition` delegates validation to the current state from `StateRegistry`. Acknowledgement requires an assignee; resolution requires at least ten characters of details. Only the reporter verifies, closes or rates. Assigned technicians and administrators can work an incident; reopening additionally allows its reporter and requires at least ten characters of justification. Feedback requires VERIFIED or CLOSED and is unique per incident.

## Time

Java uses UTC `Instant` values and PostgreSQL uses `TIMESTAMPTZ`. Hibernate's JDBC timezone is UTC. API timestamps include UTC offsets. The frontend formats timestamps in the viewer's browser timezone. Trend grouping and creation-date filters use UTC dates, labeled in the UI. Deadlines are anchored to the original creation timestamp, including priority recalculation. Existing policy edits affect new or reprioritized reports. Reopening retains deadlines and sticky breach flags.

## Security

Passwords are BCrypt hashes with cost 12. Account creation restricts public registration to Employee regardless of unexpected client fields. The factory checks the BCrypt UTF-8 byte limit. Eight-hour HMAC-signed JWTs are held in sessionStorage; deactivated users fail token authentication. CORS uses exact environment-defined origins. CSRF is disabled for the stateless bearer API, which does not authenticate with cookies. Error responses do not expose stack traces, hashes or secrets. No API edits/deletes historical audits; PostgreSQL also rejects audit UPDATE/DELETE.

See `api.md`, `design-patterns.md`, `database-design.md` and `verification.md` for implementation mapping and measured limits.
