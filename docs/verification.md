# Verification results — 22 November 2026

## Current automated checks

| Check | Result |
| --- | --- |
| `mvn clean verify` (fresh extraction, Corretto Java 21.0.13.1, PostgreSQL 17.10) | **53 passed**, zero failures/errors/skips |
| Priority strategy unit tests | 7 passed |
| Assignment strategy unit tests | 4 passed |
| State unit tests | 4 passed |
| Duplicate similarity unit tests | 4 passed |
| SLA unit tests | 6 passed |
| Observer unit test | 1 passed |
| Email recipient override unit tests | 6 passed |
| Spring Boot / MockMvc workflow and administration integration tests | 19 passed |
| PostgreSQL repository tests | 2 passed |
| `npm test` — Vitest / React Testing Library | **8 passed**, zero failures |
| `npm run build` — TypeScript and Vite | Passed; ExcelJS emits a chunk-size warning |
| `scripts/render-uml.sh` — PlantUML and Graphviz | **21 sources** rendered to SVG; primary class PNG also rendered |

Total: **61 passing automated tests**. This final pass extracted the exact submission ZIP into an empty folder, generated fresh secrets, installed dependencies with `npm ci`, initialized a separate PostgreSQL cluster, and applied all five Flyway migrations. Maven `clean verify` ran on Amazon Corretto **21.0.13.1**, matching the project's Java 21 compiler target. The earlier Java 26 verification is superseded by this clean Java 21 run.

`backend-test-results.json` records the current Surefire results. `frontend-test-results.json` records the fresh install, tests, build and dependency audits. `final-check-results.json` contains the input ZIP checksum, all 58 live API checks and restart-persistence evidence. No application source or dependency versions were changed during the final check.

## Current live application checks

- **58 HTTP requests passed** through the frontend API proxy, exercising health, authentication/registration, all three roles, admin reads, authorization rejection, incident creation/update, illegal state rejection, acknowledgement, investigation, information requests, notes, chat, notification replies/read state, duplicate confirmation, SKILL/LOAD assignment, resolution, verification, closure, feedback, combined filters, export, calendar reports and invalid inputs.
- Browser sign-in and role-specific navigation passed for employee, technician and administrator. All ten administrator management/operations pages loaded. The technician's assigned-incident controls and workload page were verified; the employee's closed incident displayed resolution, verification and five-star feedback. No browser console errors were observed.
- After stopping/restarting both the isolated backend and PostgreSQL, the 9 incidents, 8 users, 3 notes, 2 chat messages, 1 feedback and 1 confirmed duplicate were preserved. The main incident remained CLOSED with rating 5; all 53 existing audit records were retained.
- The checked-in render script successfully regenerated all **21 PlantUML sources** from the fresh extraction.
- Source files matched the tested ZIP before updating these verification documents. Local credentials and runtime/database artifacts are excluded from the submission.

## Dependency audit findings

The fresh `npm audit` reported **5 affected package entries: 2 critical and 3 moderate**. Critical entries concern `vitest`/`tinypool` test tooling; the other affected packages are `@vitest/mocker`, `exceljs` and its transitive `uuid`. The production-only audit reported **2 moderate entries and no high/critical entries**. These counts are package/advisory-chain entries, not counts of independently exploitable application flaws.

Dependency versions were left unchanged to preserve the tested behavior and avoid an unrequested major tooling migration. Passing functional tests does not clear these findings. The frontend build also reports the existing ExcelJS bundle-size warning. A previously reported historical zero-vulnerability result does not describe this current lockfile.

## Email delivery and collaboration

Email is implemented through `IncidentEmailService`, a transactional `email_outbox`, `EmailScheduler`, `SmtpOutboxService` and Spring `JavaMailSender`. When configured, the worker sends through authenticated SMTP, tracks attempts and retries, records accepted messages as SENT, and expires stale reminders. Without SMTP configuration, messages remain pending. Unmapped `.local` recipients are skipped. Historical simulated rows can remain in existing databases; they do not describe current delivery behavior.

`EMAIL_RECIPIENT_OVERRIDES` supports delivery redirection without changing login addresses. Unit tests cover the configured redirects, ordinary recipients, unmapped local addresses and inactive accounts. These tests mock database insertion; they do not send mail or prove Gmail delivery. No live inbox delivery was exercised during this verification pass; SMTP was disabled in the isolated environment to avoid sending test mail.

`IncidentChatService` persists reporter/current-technician threads through parameterized `JdbcTemplate` SQL. Flyway V3 adds chat storage and V5 adds source-notification reply links. The class diagrams and ER diagram include this implementation.

## GitHub preparation

- `.env`, `.env.*` (except `.env.example`), `.runtime/`, dependencies, build outputs, logs and local editor settings are ignored.
- README examples and email-routing test fixtures use `shared-support@example.com`; private local `.env` settings are preserved.
- The primary class diagram and complete atlas reflect all **139 declared Java types across 107 source files**. Every type is detailed on at least one numbered sheet.
- The component, sequence, deployment, use-case and ER diagrams reflect current chat and SMTP behavior.
- Native launch and Docker instructions remain in the README. Docker files are provided but not execution-verified in this pass.

## Historical browser verification (31 October 2026)


The actual React UI was exercised against the actual Java/PostgreSQL backend:

1. Employee sign-in and role-specific navigation.
2. Incident report with Payment Gateway / Application failure, impact 3, urgency 3 and 150 affected users.
3. Persisted INC-0007 returned ASSIGNED, P1, score **40.5**, a detailed calculation reason and technician Priya Sharma.
4. Technician sign-in and assigned-incident access.
5. Acknowledgement, investigation and persisted investigation note through the UI.
6. Duplicate suggestion to INC-0001 with **84% similarity** reviewed and confirmed; both incidents retained.
7. Resolution entered by the technician, verification by the reporter, closure and five-star feedback through the UI.
8. Administrator settings page saved successfully; technician skills/capacity editor saved successfully; SLA policy page displayed persisted targets. Broader admin mutations are covered by integration tests.
9. Desktop and 390 × 844 responsive dashboard inspected; mobile navigation exercised. Small-screen headings and catalog layouts refined after visual review.

Screenshots: `screenshots/admin-dashboard.png`, `screenshots/employee-closed-incident.png`, `screenshots/mobile-dashboard.png`. The administrator screenshot includes real SLA violations from elapsed demo deadlines; these are not hardcoded dashboard values.

## Historical persistence and audit protection (31 October 2026)

After the browser workflow, both **PostgreSQL and the Java backend were stopped and restarted**. Exact counts matched before and after: 7 incidents, 1 investigation note, 49 audit events, 94 in-app notifications, 1 feedback record and 1 confirmed duplicate decision. INC-0007 remained CLOSED. See `persistence-check.json` for the captured comparison. Later scheduler events may legitimately increase live counts.

An UPDATE to an audit record was attempted inside a database transaction. PostgreSQL rejected it with `Audit history is append-only`; the transaction rolled back. The application exposes no audit mutation endpoints.

## Verification limits

The browser walkthrough, screenshots and restart-persistence observations above are historical evidence, not a fresh browser regression run. A new live browser/API run and restart check are documented above. Docker (CLI unavailable), load testing, penetration testing, formal accessibility auditing and multi-instance scheduler behavior were not tested in this pass. Passing automated tests and a clean build support course demonstration and repository publication; they do not establish production operational readiness.
