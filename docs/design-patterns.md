# Design patterns

## Strategy — priority

`PriorityStrategy` defines key and calculation contracts with a typed Result (priority, score, reason). `WeightedPriorityStrategy` multiplies impact × urgency × service criticality × affected-user factor (1 / 1.2 / 1.5 at 1 / 10 / 100 affected users). `RuleBasedPriorityStrategy` uses impact × 3 + urgency × 3 + criticality × 2 + user band (0 / 3 / 6). Both use configurable descending P1/P2/P3 minimum thresholds, with P4 below P3. Equality belongs to the higher priority band.

`PriorityCalculationService` receives implementations through constructor dependency injection and selects the configured key from SystemSettings. It persists the result and explanation on Incident and selects its SLA. Incident management does not depend on either concrete algorithm. Another algorithm can implement the contract, register as a Spring component and be made selectable in the configuration validation/UI without rewriting incident orchestration.

## Strategy — technician assignment

`TechnicianAssignmentStrategy` works on Candidate records built from real Technician and Incident records. Eligible candidates must be active, available, and below maximum active workload. RESOLVED, VERIFIED and CLOSED are excluded from active workload.

`SkillBasedAssignmentStrategy` scores **100 per matched service/category skill + 2 × experience years + 3 × min(completed incidents in category,10) − 5 × active workload**. Its explanation lists matches, experience, resolved-category history, load and total score. `LoadBasedAssignmentStrategy` chooses the fewest active incidents with an email tie-break. `AssignmentService` obtains the configured implementation, persists assignment history and publishes events. Manual overrides still enforce availability/capacity, under a technician lock.

This separates routing policy from incident lifecycle and provides meaningful runtime polymorphism. The system falls back to a CLASSIFIED unassigned incident when no candidate is eligible.

## State

`IncidentState` has `status()` and `transition(Incident, Status)`. `AbstractIncidentState` holds immutable current/successor sets and common prerequisites. `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `ResolvedState`, `VerifiedState` and `ClosedState` each declare their legal successors. `StateRegistry` maps statuses to injected state objects. `Incident.transition` delegates before mutating status.

The action dispatcher in IncidentService selects the requested target and enforces actor permissions/details; it does not contain the graph of legal transitions. This keeps action payload/authorization concerns separate from the state graph. Adding a state involves its class, enum, registry discovery, relevant service action and UI, without replacing the transition validator.

## Observer

`IncidentEventPublisher` stores an injected immutable list of `IncidentEventListener`. `AuditObserver` appends AuditLog. `NotificationObserver` selects recipients, including administrators on SLA escalation, and delegates delivery channels. Important creation, classification, priority, recommendation, assignment, acknowledgement, investigation, notes, information requests, resolution, verification, closure, reopening, feedback and SLA changes publish events.

Observers run synchronously in the business transaction. They make audit history, in-app notifications and queued email atomic with the mutation. `IncidentEmailService` is an event listener that inserts outbox rows; `EmailScheduler` invokes `SmtpOutboxService` to send through `JavaMailSender` after commit, with expiry checks and bounded retries. Add another listener without modifying IncidentService.

## Factory

`UserFactory.create` validates password byte/length limits, hashes with the injected encoder and creates the appropriate Employee, Technician or Administrator subtype. Public registration fixes the role to Employee; administrators manage other roles. The factory centralizes secure construction and keeps hashing out of controllers/domain objects.

`NotificationObserver` handles in-app delivery. `IncidentEmailService` observes the same incident events and queues assignment and status-change emails based on persisted settings. SMTP delivery runs in a separate worker.

## Repository

Fifteen Spring Data interfaces extend JpaRepository; IncidentRepository also extends JpaSpecificationExecutor. Runtime proxies implement common persistence operations; named query methods and a locked incident/technician query express domain access needs. Services coordinate repositories inside transactions, while entities contain no SQL or repository dependencies. This separates storage from policy and permits focused strategy/state unit tests plus real PostgreSQL integration tests.
