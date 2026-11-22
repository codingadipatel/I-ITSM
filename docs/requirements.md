# Requirements and implementation checklist

This checklist maps the supplied 27-section brief to actual deliverables. Verification status is recorded separately in `verification.md`; operational production readiness is not implied by a checked implementation.

- [x] React/TypeScript/Vite/Tailwind/Router/Axios/Recharts frontend with connected forms, responsive layouts, loading/empty/error states and dialogs.
- [x] Java 21/Spring Boot/Web/Security/JPA/Validation/Maven/JWT backend and PostgreSQL database.
- [x] Public landing, login, registration; employee/technician/admin navigation and permissions.
- [x] Personal/global dashboards from scoped database queries.
- [x] Employee create/list/detail/update-before-acknowledgement, notes, replies, assigned technician, verification, closure, feedback, notifications and profile.
- [x] Technician assigned queue, deadlines, acknowledge/investigate/notes/request-information/resolve, workload, skills and history.
- [x] Admin people/technician create/edit/deactivate, skills, availability and capacity; catalog create/edit/deactivate; policies/strategies/thresholds; monitoring, reassignment, duplicate review, audit, reports and notification settings.
- [x] Incident service layer, persistence, DTO validation, typed errors and controlled domain methods.
- [x] Priority Strategy pattern with weighted and rule-based implementations; score, reason, thresholds, recalculation and audit.
- [x] Assignment Strategy pattern with skill/history/experience/load and least-load implementations; recommendation reason, eligibility, capacity lock, override, history and notification.
- [x] Default SLA policies (5/30,15/120,60/480,240/1440 minutes); UTC timestamps, server monitoring, response/resolution tracking, warnings, violations and escalation.
- [x] State pattern with all eight named states, legal successors, prerequisites, verification and justified reopening.
- [x] Observer pattern with IncidentEventPublisher, IncidentEventListener, NotificationObserver and AuditObserver.
- [x] In-app notification channel and SMTP email for assignments, SLA milestones, and status changes with an admin outbox.
- [x] UserFactory; Spring Data Repository pattern.
- [x] Real duplicate detection (30-day Jaccard + service/category), stored scores, confirm/reject and record preservation.
- [x] Real smart recommendation using stored skills, availability, workload, experience and completed-category counts.
- [x] Persistent chronological immutable audit history, including scheduled system actors.
- [x] Combined incident search: ID/text, dates, status, priority, category, service, technician, SLA level; pagination.
- [x] Actual-data incident totals/open/critical/resolved/closed, SLA breaches/compliance, response/resolution averages, workloads, category/service/priority distributions and trends.
- [x] Normalized schema, foreign keys/checks/indexes, appropriate ownership/associations, Flyway, optional demo seeding and restart persistence.
- [x] OOP concepts and patterns documented against real classes; encapsulation, abstraction, inheritance, polymorphism, composition, association, aggregation, collections and exceptions.
- [x] BCrypt, environment secrets, JWT, exact CORS, server permissions and ownership checks; no hash/secret API exposure.
- [x] Unit/integration/repository/frontend tests including invalid inputs and access checks; more than 15 documented cases.
- [x] Eight separate PlantUML sources and eight SVG renders: use case, classes, two sequences, activity, component, deployment and ER.
- [x] README, architecture/database/OOP/pattern/test/technology documents, API reference, setup scripts, environment example and Docker packaging.

## Deliberate schema/design adaptations

User subtypes use one users table; technician skills use a many-to-many join; SlaTracking is embedded into incidents; incident_history is represented by audit_logs and notes rather than duplicating the same event stream. Assignment strategy and priority settings share one singleton settings row. These choices are implemented and reflected in UML.

## Functional alternatives and boundaries

SMTP delivery requires sender configuration. No destructive incident/user deletion is offered; account/catalog deactivation and audit preservation satisfy the lifecycle. Duplicate details are restricted to authorized support staff to avoid revealing another employee's incidents. Admin policy creation is available by API; the UI edits all four preseeded unique-priority policies. Role dashboards share a design shell but have different navigation, data scope and available actions.

Production infrastructure hardening, multi-node scheduling, business-time calendars, attachments and semantic AI are outside the demonstrated implementation. Docker files are provided but were not executed because Docker was unavailable.
