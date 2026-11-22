# UML deliverables

Updated on 22 November 2026 against the current production Java source and Flyway migrations V1–V5.

## Start here

- [Incident, users and SLA](class-diagram.svg): primary class diagram; [editable PlantUML](class-diagram.puml) and [PNG](class-diagram.png).
- [Complete project class map](class-atlas/00-complete-project.svg): all 139 declared Java types across 107 source files on one zoomable canvas.
- [Detailed class atlas PDF](class-atlas/I-ITSM-Complete-UML.pdf): contents and twelve bookmarked diagrams. Large vector pages preserve readable signatures when zoomed.
- [Class index](class-atlas/class-index.csv): source location and detailed sheet for every declared type.

## Detailed class sheets

Each numbered diagram has a matching `.puml` source and `.svg` render under `class-atlas/`.

| Sheet | Diagram |
| --- | --- |
| 01 | [Incident, User subtypes, catalogs and SLA](class-atlas/01-incident-users.svg) |
| 02 | [History, collaboration and supporting records](class-atlas/02-incident-history.svg) |
| 03 | [Priority and assignment strategies](class-atlas/03-strategies.svg) |
| 04 | [State pattern and lifecycle](class-atlas/04-state-lifecycle.svg) |
| 05 | [Events, notifications, SMTP and scheduling](class-atlas/05-events-email.svg) |
| 06 | [Incident application services](class-atlas/06-incident-services.svg) |
| 07 | [User, authentication and configuration services](class-atlas/07-account-services.svg) |
| 08 | [REST API and security](class-atlas/08-api-security.svg) |
| 09 | [All repository interfaces](class-atlas/09-repositories.svg) |
| 10 | [Request DTOs](class-atlas/10-request-models.svg) |
| 11 | [Response DTOs](class-atlas/11-response-models.svg) |
| 12 | [Exceptions and initialization](class-atlas/12-infrastructure-errors.svg) |

The inventory covers classes, interfaces, enums and nested records. Constructors and generated Lombok/record boilerplate are omitted. Explicitly written methods and private helpers remain. The frontend uses React functions, which are not represented as Java classes. Context types are compact on individual sheets and detailed elsewhere.

## Supporting diagrams

| Diagram | Editable source | Rendered SVG |
| --- | --- | --- |
| Use cases | [PlantUML](use-case-diagram.puml) | [SVG](use-case-diagram.svg) |
| Incident reporting sequence | [PlantUML](sequence-diagram-incident-reporting.puml) | [SVG](sequence-diagram-incident-reporting.svg) |
| Technician assignment sequence | [PlantUML](sequence-diagram-technician-assignment.puml) | [SVG](sequence-diagram-technician-assignment.svg) |
| Incident lifecycle activity | [PlantUML](activity-diagram-incident-lifecycle.puml) | [SVG](activity-diagram-incident-lifecycle.svg) |
| Components | [PlantUML](component-diagram.puml) | [SVG](component-diagram.svg) |
| Deployment | [PlantUML](deployment-diagram.puml) | [SVG](deployment-diagram.svg) |
| Database ER | [PlantUML](database-er-diagram.puml) | [SVG](database-er-diagram.svg) |

The supporting views include persisted chat, notification replies, the SMTP outbox worker and retry/reminder metadata. SMTP requires environment configuration; queueing alone does not prove delivery.

## Render

Install Java, an official PlantUML JAR and Graphviz (`dot` on PATH), then run from the project root:

```bash
PLANTUML_JAR=/absolute/path/plantuml.jar ./scripts/render-uml.sh
```

The script renders all 21 PlantUML sources plus the primary class PNG. Supporting diagrams use Smetana; class diagrams use Graphviz. The PDF is a checked-in source snapshot, so update it separately when class diagrams change. `source-manifest.json` records the source hashes for the snapshot; `class-inventory.json` uses repository-relative file locations.
