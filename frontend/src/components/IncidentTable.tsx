import { Link } from "react-router-dom";
import { ArrowUpRight, Clock3 } from "lucide-react";
import type { Incident } from "../types";
import { Badge, Empty } from "./UI";
import { initials, remaining, date } from "../utils/format";
export function IncidentTable({
  incidents,
  compact = false,
  filters,
}: {
  incidents: Incident[];
  compact?: boolean;
  filters?: Record<string, import("react").ReactNode>;
}) {
  return (
    <div className="table-wrap">
      <table className="incident-table">
        <thead>
          <tr>
            {["Incident", "Affected service", "Created", "Priority", "Status", ...(compact ? [] : ["Assigned to"]), "SLA deadline"].map(name =>
              <th key={name} scope="col">{name}{filters?.[name]}</th>)}
            <th aria-label="Open incident" />
          </tr>
        </thead>
        <tbody>
          {!incidents.length && <tr><td colSpan={compact ? 7 : 8}><Empty title="No incidents found" text="Try changing your filters, or report a new incident." /></td></tr>}
          {incidents.map((i) => (
            <tr key={i.id}>
              <td>
                <Link className="incident-cell" to={`/app/incidents/${i.id}`}>
                  <span className="incident-ref">
                    INC-{String(i.id).padStart(4, "0")}
                  </span>
                  <strong>{i.title}</strong>

                </Link>
              </td>
              <td>{i.service.name}</td>
              <td><time dateTime={i.createdAt}>{date(i.createdAt)}</time></td>
              <td>
                <Badge value={i.priority} type="priority" />
              </td>
              <td>
                <Badge value={i.status} />
              </td>
              {!compact && (
                <td>
                  {i.technician ? (
                    <div className="person-cell">
                      <span className="avatar tiny">
                        {initials(i.technician.name)}
                      </span>
                      <span>{i.technician.name}</span>
                    </div>
                  ) : (
                    <span className="muted">Unassigned</span>
                  )}
                </td>
              )}
              <td>
                <span
                  className={`sla-inline ${i.slaLevel === "VIOLATED" ? "danger" : i.slaLevel === "AT_RISK" ? "warning" : ""}`}
                >
                  <Clock3 size={13} />
                  {remaining(i.resolutionDeadline, !!i.resolvedAt)}
                </span>
              </td>
              <td>
                <Link
                  to={`/app/incidents/${i.id}`}
                  className="icon-button"
                  aria-label={`Open incident ${i.id}`}
                >
                  <ArrowUpRight size={17} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
