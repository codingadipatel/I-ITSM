# REST API

Base URL: `/api`. JSON request/response. Use `Authorization: Bearer <token>` after login/register. Successful authentication returns `{token,user}`. A UserView includes id/name/email/role/status and applicable technician fields; password hashes are never returned. Errors contain `status`, `message`, `timestamp`; unauthenticated JWT filter errors contain a safe `message`.

E = Employee, T = Technician, A = Administrator. Incident ownership checks are applied in addition to the table below. T can only read/work assigned incidents (or incidents they personally reported). A can monitor and work all incidents; only an incident's actual reporter can verify/close/rate it.

## Authentication and users

| Method / route | Permission | Body or result |
| --- | --- | --- |
| POST /auth/register | Public | `{name,email,password}`; always creates Employee, 201 |
| POST /auth/login | Public | `{email,password}`; 200 Auth |
| GET /users/me | Authenticated | Current UserView |
| PUT /users/me | Authenticated | `{name}` |
| GET /users | A | UserView[] |
| POST /users | A | UserInput, 201 |
| PUT /users/{id} | A | UserInput; role/email immutable |
| DELETE /users/{id} | A | Soft deactivate, 204; self-deactivation forbidden |
| GET /technicians | A/T | All technicians for A; own profile for T |
| GET /technicians/{id}/workload | A or that T | Technician and active workload |
| GET /technicians/{id}/recommendations?incidentId={id} | A | Recommendation if this technician is the configured algorithm's top candidate |

UserInput: `{name,email,password?,role,active,available,maxWorkload,experienceYears,skills:["Network"]}`. Password required on creation; existing password is preserved on updates. Capacities 1–100; experience 0–60. Public registration passwords 10–72 characters and at most 72 UTF-8 bytes in the factory.

## Incidents

IncidentInput: `{title,description,serviceId,categoryId,impact,urgency,affectedUsers}`. Title 5–180 characters, description 15–10000, impact/urgency 1–3, users 1–1000000, active existing catalogs required.

| Method / route | Permission | Behavior |
| --- | --- | --- |
| POST /incidents | E/T/A | Report, classify, prioritize, detect duplicates, auto-assign; 201 |
| GET /incidents | E/T/A | Scoped pageable search |
| GET /incidents/{id} | Authorized reader | `{incident,notes,history,duplicates,feedback}`; duplicate details hidden from employees to prevent cross-reporter information disclosure |
| PUT /incidents/{id} | Reporter before acknowledgement; A while active | IncidentInput; recalculate priority/deadlines and audit |
| GET /incidents/{id}/recommendations | Assigned T / A | `[{technicianId,technicianName,reason,score}]` |
| POST /incidents/{id}/assign | A | `{technicianId,reason}`; reason 3–255; enforce availability/capacity |
| POST /incidents/{id}/acknowledge | Assigned T / A | ASSIGNED → ACKNOWLEDGED |
| POST /incidents/{id}/investigate | Assigned T / A | ACKNOWLEDGED → IN_PROGRESS |
| POST /incidents/{id}/notes | Authorized reader | `{details}` 3–10000; forbidden when closed; 201 |
| POST /incidents/{id}/request-information | Assigned T / A | `{details}`; visible to reporter; notification |
| POST /incidents/{id}/resolve | Assigned T / A | `{details}` at least 10 characters; IN_PROGRESS → RESOLVED |
| POST /incidents/{id}/verify | Reporter | `{details}` at least 3 characters; RESOLVED → VERIFIED |
| POST /incidents/{id}/close | Reporter | VERIFIED → CLOSED |
| POST /incidents/{id}/reopen | Reporter / assigned T / A | `{details}` at least 10 characters; RESOLVED/VERIFIED/CLOSED → IN_PROGRESS |
| POST /incidents/{id}/feedback | Reporter | `{rating:1..5,comment?}`; only VERIFIED/CLOSED, unique; 201 |
| POST /incidents/{id}/duplicates/{suggestionId} | Assigned T / A | `{decision:"CONFIRMED"|"REJECTED"}`; retain both incidents |

Search query parameters combine with AND: `q` (text or `INC-ID`), `id`, `status`, `priority`, `categoryId`, `serviceId`, `technicianId`, `slaLevel` (SAFE/APPROACHING/AT_RISK/VIOLATED), `from` and `to` (inclusive UTC YYYY-MM-DD dates), `page` (zero-based), `size` (1–100). Results use Spring Page fields: `content,totalElements,totalPages,number,...`; default ordering is newest created first.

## Catalogs and configuration

| Method / route | Permission | Behavior |
| --- | --- | --- |
| GET /services, GET /categories | E/T/A | Catalog[]; inactive entries only visible to A |
| POST /services, POST /categories | A | CatalogInput |
| PUT /services/{id}, PUT /categories/{id} | A | CatalogInput; deactivate with active=false |
| GET /settings | A | Active strategy/threshold/channel settings |
| PUT /settings | A | Settings input; strictly descending positive thresholds |
| GET /sla/policies | A | Policy[] |
| POST /sla/policies | A | `{priority,responseMinutes,resolutionMinutes}`; priority unique |
| PUT /sla/policies/{id} | A | Targets; priority immutable; resolution >= response |
| GET /sla/violations?page=0 | A | Pageable persisted violations |

CatalogInput: `{name,active,criticality:1..3,requiredSkill}`. Categories ignore criticality but share input validation. Settings: `{assignmentStrategy:"SKILL"|"LOAD",priorityStrategy:"WEIGHTED"|"RULE",p1Threshold,p2Threshold,p3Threshold,emailEnabled}`.

## Notifications, reports and audit

| Method / route | Permission | Behavior |
| --- | --- | --- |
| GET /notifications | Authenticated | Only recipient's messages |
| PATCH /notifications/{id}/read | Recipient | Mark one read; others receive 403 |
| GET /email-outbox?page=0 | A | Simulated delivery records, pageable |
| GET /audit-logs?page=0 | A | Read-only pageable system-wide events |
| GET /reports/overview | E/T/A | Scope-aware totals, response/resolution averages, compliance and priority distribution |
| GET /reports/technician-workload | A/T | All workloads for A; own for T |
| GET /reports/category-analysis | E/T/A | Scope-aware `{name,value}[]` |
| GET /reports/service-analysis | E/T/A | Scope-aware `{name,value}[]` |
| GET /reports/incident-trends | E/T/A | Last 14 UTC dates, including zero days |
| GET /health | Public | `{status:"UP"}` |

Overview averages use timestamps for incidents that have acknowledged/resolved records; minutes are rounded. SLA compliance is the percentage of scoped incidents with neither persisted response nor resolution breach, including currently active incidents. Reopened incidents whose resolution timestamp is reset are excluded from current resolved totals/averages until resolved again. These are clearly defined course metrics, not an availability SLI.

HTTP: 400 invalid request, 401 missing/invalid authentication, 403 forbidden resource/action, 404 missing incident/service, 409 invalid lifecycle/capacity/uniqueness/concurrent change, 500 safe unexpected error. No incident/audit hard-delete endpoint is exposed, preserving history.
