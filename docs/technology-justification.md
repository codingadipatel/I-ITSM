# Technology justification

- Java 21 provides a stable language target, records for immutable DTOs, modern switch expressions and a mature OOP ecosystem. Spring Boot assembles Web, Security, Validation, JPA and scheduling with constructor injection and explicit transaction boundaries.
- PostgreSQL supplies durable relational constraints, transactions, locks, timezone-aware timestamps, indexes and database-level audit protection. The project intentionally does not substitute an in-memory database for integration tests.
- Flyway gives reproducible, versioned schema setup. Hibernate schema validation catches mapping drift instead of silently changing production tables.
- React/TypeScript separates reusable interaction controls and API contracts. Vite provides fast development and deterministic builds; Tailwind 4 is integrated alongside a coherent CSS design system. React Router supplies URL-based navigation and protected nested layouts.
- Axios standardizes bearer authentication, timeouts and API failures. Recharts displays real API aggregates without frontend-generated statistics. Lucide provides a consistent vector icon language.
- JUnit 5 and Mockito isolate algorithm/state/observer behavior; Spring Boot Test/MockMvc and PostgreSQL validate authorization, transactions, repository mappings and workflows. Vitest and React Testing Library exercise user-visible forms and route behavior.
- PlantUML keeps diagrams editable and versionable. SVG renders preserve readable zoom for the detailed class and ER diagrams. The overview/use-case/sequence/ER diagrams use Smetana; the source-verified class diagrams use Graphviz dot for layout.

Primary references consulted: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [PostgreSQL date/time documentation](https://www.postgresql.org/docs/current/datatype-datetime.html). The implementation uses Java Instant plus TIMESTAMPTZ to preserve absolute times and explicitly formats them for local viewers.
