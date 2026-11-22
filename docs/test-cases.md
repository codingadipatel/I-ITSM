# Test cases

Automated tests execute real logic; frontend API mocks isolate transport behavior, while backend integration/repository tests use PostgreSQL. Backend tests run in rollback transactions in a separate test database. Boundary fixtures use fixed UTC instants. See `verification.md` for final counts/results.

| # | Area | Input / action | Expected result | Coverage |
| --- | --- | --- | --- | --- |
| 1 | Priority calculation | Critical 3×3×3×1.5 | P1, score 40.5 and explanation | PriorityStrategyTest |
| 2 | Priority boundaries | 9, 10, 99 and 100 affected-user bands | User factors follow documented bands | PriorityStrategyTest |
| 3 | Threshold equality | Score exactly at configured threshold | Higher priority selected | PriorityStrategyTest |
| 4 | Invalid settings | P1 <= P2 or nonpositive band | Validation rejects update | PriorityStrategyTest / API |
| 5 | Load assignment | Two eligible different workloads | Least-loaded selected | AssignmentStrategyTest |
| 6 | Skill assignment | Matching expert with slightly higher load | Skill score wins, reason present | AssignmentStrategyTest |
| 7 | Availability/capacity | Unavailable or full technician | Excluded from recommendations | AssignmentStrategyTest |
| 8 | User status | Inactive technician | Excluded even if available flag true | AssignmentStrategyTest |
| 9 | State graph | NEW → CLOSED | InvalidIncidentStateException; state unchanged | IncidentStateTest |
| 10 | Prerequisite | Unassigned acknowledgement | Rejected | IncidentStateTest |
| 11 | Resolution content | Short details | Rejected before commit | IncidentStateTest / integration |
| 12 | Lifecycle | Report through feedback | Closed, notes/audit/notifications persisted | IncidentWorkflowTest |
| 13 | Reopening | Short versus justified reason | Short rejected; valid goes IN_PROGRESS | IncidentWorkflowTest |
| 14 | Duplicate normalization | Case and punctuation changes | Equivalent tokens score 1 | DuplicateDetectionTest |
| 15 | Duplicate boundary | Disjoint/empty/partial token sets | 0/0/0.5 Jaccard respectively | DuplicateDetectionTest |
| 16 | Duplicate workflow | Two equivalent recent incidents | Stored score 1; authorized review persists | IncidentWorkflowTest |
| 17 | Duplicate access | Employee tries confirm | 403, both records preserved | IncidentWorkflowTest |
| 18 | SLA calculation | P2 at fixed UTC instant | Response +15m, resolution +120m | SlaTrackingTest |
| 19 | SLA warning bands | Deadline within 10m and 2m | APPROACHING / AT_RISK | SlaTrackingTest |
| 20 | SLA exact deadline | Now equals deadline, then one second after | Not breached at equality; violated after | SlaTrackingTest |
| 21 | SLA completion | Acknowledged/resolved before target | No later false breach | SlaTrackingTest |
| 22 | SLA history | Replan after a response violation | Breach flag remains true | SlaTrackingTest |
| 23 | Escalation | Overdue incident scanned twice | One escalation for unchanged breach state | IncidentWorkflowTest |
| 24 | Observer | Two registered listeners | Both receive actual event | EventPublisherTest (Mockito) |
| 25 | Ownership | Other employee reads detail/list | 403 detail, no leaked list items | IncidentWorkflowTest |
| 26 | Authorization | Employee manages users or acknowledges | 403 | IncidentWorkflowTest |
| 27 | Authentication | No token, invalid login, deactivated JWT | 401 | IncidentWorkflowTest |
| 28 | Registration | Client requests ADMIN through public route | Created as EMPLOYEE; hash hidden | IncidentWorkflowTest |
| 29 | Error handling | Invalid fields, missing ID, invalid enum filter | 400 / 404 / 400 safe consistent errors | IncidentWorkflowTest |
| 30 | Search | Priority + status + catalog + page size | Correct combined result | IncidentWorkflowTest |
| 31 | Notification privacy | Another recipient marks read | 403 | IncidentWorkflowTest |
| 32 | Admin technician CRUD | Create skills, change availability, deactivate | Persisted account changes | IncidentWorkflowTest |
| 33 | Admin configuration | Catalog, SLA, LOAD/RULE and email disabled | Persisted; in-app remains, email omitted | IncidentWorkflowTest |
| 34 | No eligible technician | Report while only technician unavailable | CLASSIFIED, manual unavailable assignment 409 | IncidentWorkflowTest |
| 35 | Other technician | Unassigned tech requests information | 403 | IncidentWorkflowTest |
| 36 | Repository mapping | Persist skill association, clear persistence context | Reloaded subtype/skills/capacity correct | RepositoryPersistenceTest |
| 37 | Repository lookup | Mixed-case email lookup | Account found case-insensitively | RepositoryPersistenceTest |
| 38 | Frontend login | Submit credentials, backend rejects | Error visible; correct payload | app.test.tsx |
| 39 | Frontend registration | Short password | Form invalid; no request | app.test.tsx |
| 40 | Frontend roles | Employee requests admin route; unauthenticated private route | Redirects to workspace/login | app.test.tsx |
| 41 | Frontend incident | Complete required fields then empty form | POST correct DTO; empty form blocked | app.test.tsx |
| 42 | Frontend filter | Select priority and status | Query URL combines filters | app.test.tsx |
| 43 | Frontend dashboard | Server returns known metrics | Current name and API metrics render | app.test.tsx |
| 44 | Restart persistence | Restart backend and database | Incident IDs/counts/notes/audits retained | Live smoke verification |
| 45 | Browser walkthrough | Employee report; tech resolves; employee verifies/closes | Visible final state and feedback | Browser QA |

## Execution

`./scripts/test-backend.sh` runs all JUnit/Mockito/MockMvc/PostgreSQL tests. `cd frontend && npm test && npm run build` runs React Testing Library and TypeScript/Vite checks. A passing browser walkthrough supplements automated component/API tests; it is not represented as an automated end-to-end test suite. Docker validation remains explicitly unexecuted in this environment.
