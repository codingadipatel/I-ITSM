# Database design

Flyway owns schema changes: `backend/src/main/resources/db/migration/V1__initial_schema.sql` defines tables, constraints, indexes and append-only audit protection. V2 seeds reference catalogs/policies/settings. V3 adds incident chat, V4 adds SMTP retry/reminder metadata, and V5 links chat replies to source notifications. Hibernate validates the schema at boot (`ddl-auto: validate`); it does not recreate it. Sample/reference SQL is also in `database/sample-data.sql` for inspection, and must not be reapplied after Flyway.

| Table | Purpose and relationships |
| --- | --- |
| users | Single-table User inheritance; role discriminator EMPLOYEE / TECHNICIAN / ADMIN, unique case-insensitive email, password hash, account status, technician capacity/availability/experience |
| skills | Reusable skill names |
| technician_skills | Many-to-many association with composite technician/skill key |
| services | Reusable service, active flag, 1–3 business criticality and required skill |
| incident_categories | Reusable category, active flag and required skill |
| incidents | Reporter, current assignee, service, category, score/priority/reasons, state, optimistic version, timestamps, resolution and verification |
| sla_policies | One policy per priority with validated minute targets |
| system_settings | Singleton active assignment/priority strategies, strictly ordered thresholds, email setting |
| incident_assignments | Assignment history: incident, technician, assigning user, reason, timestamp |
| incident_notes | Shared conversation/investigation entries and information requests with author/time |
| audit_logs | Append-only incident/administrative events; optional actor for scheduled work |
| feedback | One record per incident, rating constrained to 1–5 |
| duplicate_suggestions | Two preserved incidents, similarity 0–1, review status/actor/time; no self-links or repeated ordered pairs |
| notifications | Recipient-specific persisted message and read status |
| incident_chat_messages | Incident/current-technician conversation thread, sender, message/time and optional source notification |
| email_outbox | Queued SMTP messages, retry and delivery status, and unique SLA reminder keys |

`SlaTracking` is an embedded value component. Its response/resolution deadlines, acknowledgement/resolution timestamps, sticky breach flags and current warning level are columns on incidents. A separate one-to-one table would add joins without improving this ownership model. Likewise, Employee and Administrator need no duplicate profile tables; subtype identity is persisted by the users discriminator. The many-to-many technician-skills link is a real relation, without an artificial link entity.

All generated IDs are bigint identities. Relationships reference retained rows; application deletion means account/catalog deactivation, preserving incident and history foreign keys. No destructive cascades are used. Notes, assignments, audits and feedback have independent identities and reference their parent incident. Timestamps are timezone-aware. The model avoids copying catalog names into incident rows; score/explanation/deadlines are intentional decision snapshots.

Indexes support reporter/time listing, technician/state workload, common status/priority/category/service combinations, active resolution deadlines, audit chronology and notification recipient/time. JPA Specifications combine filters with mandatory role scope and bounded page size (1–100). Table-level checks protect impact/urgency, criticality, ratings, SLA target order, duplicate scores and threshold order. A database trigger rejects audit UPDATE and DELETE; migrations or a database superuser remain outside ordinary user permissions.

## Setup and persistence

Use Docker Compose's named volume or `scripts/local-db.sh`, which creates a local `.runtime/postgres` cluster on port 55432. `.env` supplies JDBC credentials. On boot Flyway applies migrations and Hibernate validates mappings. Demo initialization is optional, guarded by `DEMO_ENABLED` and an empty users table. Demo account hashes are computed from the environment, never shipped as SQL credentials.

Stopping/restarting the backend or database keeps all records. Back up the database with `pg_dump`; restore into an empty database and let Flyway validate its history. Do not rerun seed SQL or delete volumes to restart an application. See `verification.md` for the restart-persistence check and `uml/database-er-diagram.svg` for the rendered schema.
