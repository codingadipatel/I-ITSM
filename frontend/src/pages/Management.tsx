import { useState, type FormEvent } from "react";
import {
  Plus,
  Edit3,
  UserRoundCog,
  Users,
  Layers,
  Shapes,
  Power,
  Search,
  ArrowUpRight,
  Check,
  Workflow,
  Sparkles,
  Clock3,
  ShieldCheck,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { useToast } from "../context/ToastContext";
import { api, errorMessage } from "../services/api";
import type {
  User,
  Catalog,
  Policy,
  Settings,
  Metric,
  Audit,
  Page,
} from "../types";
import {
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Modal,
  Confirm,
  Badge,
} from "../components/UI";
import { label, date, initials } from "../utils/format";
type UserForm = {
  name: string;
  email: string;
  password: string;
  role: string;
  active: boolean;
  available: boolean;
  maxWorkload: number;
  experienceYears: number;
  skills: string;
};
export function People({
  techniciansOnly = false,
}: {
  techniciansOnly?: boolean;
}) {
  const query = useApi<User[]>("/users");
  const toast = useToast();
  const [edit, setEdit] = useState<User | null | undefined>();
  const [deactivate, setDeactivate] = useState<User | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [form, setForm] = useState<UserForm>({
    name: "",
    email: "",
    password: "",
    role: "EMPLOYEE",
    active: true,
    available: true,
    maxWorkload: 8,
    experienceYears: 0,
    skills: "",
  });
  function open(user: User | null) {
    setEdit(user);
    setError("");
    setForm(
      user
        ? { ...user, password: "", skills: user.skills.join(", ") }
        : {
            name: "",
            email: "",
            password: "",
            role: techniciansOnly ? "TECHNICIAN" : "EMPLOYEE",
            active: true,
            available: true,
            maxWorkload: 8,
            experienceYears: 0,
            skills: "",
          },
    );
  }
  async function save(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const input = {
        ...form,
        password: form.password || undefined,
        skills: form.skills
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean),
      };
      await (edit
        ? api.put(`/users/${edit.id}`, input)
        : api.post("/users", input));
      toast("Account saved");
      setEdit(undefined);
      query.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  const rows =
    query.data?.filter(
      (u) =>
        (!techniciansOnly || u.role === "TECHNICIAN") &&
        (u.name + " " + u.email).toLowerCase().includes(search.toLowerCase()),
    ) || [];
  return (
    <>
      <PageHeading
        eyebrow="TEAM MANAGEMENT"
        title={techniciansOnly ? "Technicians" : "People"}
        description={
          techniciansOnly
            ? "Match your team’s expertise and capacity to the work ahead."
            : "Manage the people behind your service operations."
        }
        action={
          <button className="btn primary" onClick={() => open(null)}>
            <Plus size={17} />
            {techniciansOnly ? "Add technician" : "Add person"}
          </button>
        }
      />
      <section className="panel">
        <div className="filter-toolbar">
          <div className="search-field">
            <Search size={17} />
            <input
              aria-label="Search people"
              placeholder="Search by name or email…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <span className="muted">{rows.length} people</span>
        </div>
        {query.loading ? (
          <Loading />
        ) : query.error ? (
          <ErrorState message={query.error} retry={query.reload} />
        ) : rows.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Person</th>
                  <th>Role</th>
                  <th>Status</th>
                  {techniciansOnly && (
                    <>
                      <th>Skills</th>
                      <th>Capacity</th>
                    </>
                  )}
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="person-cell">
                        <span className="avatar">{initials(u.name)}</span>
                        <div>
                          <strong>{u.name}</strong>
                          <small>{u.email}</small>
                        </div>
                      </div>
                    </td>
                    <td>{label(u.role)}</td>
                    <td>
                      <Badge
                        value={
                          !u.active
                            ? "INACTIVE"
                            : u.role === "TECHNICIAN" && !u.available
                              ? "UNAVAILABLE"
                              : "ACTIVE"
                        }
                      />
                    </td>
                    {techniciansOnly && (
                      <>
                        <td>
                          <div className="skill-list">
                            {u.skills.map((s) => (
                              <span key={s}>{s}</span>
                            ))}
                          </div>
                        </td>
                        <td>
                          {u.maxWorkload} max · {u.experienceYears}y experience
                        </td>
                      </>
                    )}
                    <td>
                      <div className="row-actions">
                        <button
                          className="icon-button"
                          aria-label={`Edit ${u.name}`}
                          onClick={() => open(u)}
                        >
                          <Edit3 size={16} />
                        </button>
                        {u.active && (
                          <button
                            className="icon-button"
                            aria-label={`Deactivate ${u.name}`}
                            onClick={() => setDeactivate(u)}
                          >
                            <Power size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title="No people found"
            text="Add someone or update your search."
          />
        )}
      </section>
      {edit !== undefined && (
        <Modal
          title={
            edit
              ? "Edit account"
              : techniciansOnly
                ? "Add technician"
                : "Add person"
          }
          onClose={() => setEdit(undefined)}
        >
          <form onSubmit={save}>
            <label>
              Full name
              <input
                required
                maxLength={120}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </label>
            <label>
              Email address
              <input
                required
                type="email"
                maxLength={200}
                disabled={!!edit}
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </label>
            {!edit && (
              <label>
                Initial password
                <input
                  required
                  type="password"
                  minLength={10}
                  maxLength={72}
                  autoComplete="new-password"
                  value={form.password}
                  onChange={(e) =>
                    setForm({ ...form, password: e.target.value })
                  }
                />
              </label>
            )}
            <div className="form-grid">
              <label>
                Role
                <select
                  disabled={!!edit || techniciansOnly}
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                >
                  {["EMPLOYEE", "TECHNICIAN", "ADMIN"].map((r) => (
                    <option key={r} value={r}>
                      {label(r)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Account status
                <select
                  value={String(form.active)}
                  onChange={(e) =>
                    setForm({ ...form, active: e.target.value === "true" })
                  }
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </label>
            </div>
            {form.role === "TECHNICIAN" && (
              <>
                <label>
                  Technical skills
                  <small className="muted">
                    Comma-separated; match service and category skill names.
                  </small>
                  <input
                    maxLength={500}
                    value={form.skills}
                    onChange={(e) =>
                      setForm({ ...form, skills: e.target.value })
                    }
                    placeholder="Network, Database, Applications"
                  />
                </label>
                <div className="form-grid">
                  <label>
                    Maximum active workload
                    <input
                      required
                      type="number"
                      min={1}
                      max={100}
                      value={form.maxWorkload}
                      onChange={(e) =>
                        setForm({ ...form, maxWorkload: +e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Experience (years)
                    <input
                      required
                      type="number"
                      min={0}
                      max={60}
                      value={form.experienceYears}
                      onChange={(e) =>
                        setForm({ ...form, experienceYears: +e.target.value })
                      }
                    />
                  </label>
                </div>
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={form.available}
                    onChange={(e) =>
                      setForm({ ...form, available: e.target.checked })
                    }
                  />
                  Available for assignments
                </label>
              </>
            )}
            {error && (
              <div role="alert" className="inline-error">
                {error}
              </div>
            )}
            <div className="form-actions">
              <button
                type="button"
                className="btn secondary"
                onClick={() => setEdit(undefined)}
              >
                Cancel
              </button>
              <button className="btn primary" disabled={busy}>
                {busy ? "Saving…" : "Save person"}
              </button>
            </div>
          </form>
        </Modal>
      )}
      {deactivate && (
        <Confirm
          title="Deactivate account"
          text={`${deactivate.name} will no longer be able to sign in or receive new assignments. Their records are preserved.`}
          busy={busy}
          onClose={() => setDeactivate(null)}
          onConfirm={async () => {
            setBusy(true);
            try {
              await api.delete(`/users/${deactivate.id}`);
              toast("Account deactivated");
              setDeactivate(null);
              query.reload();
            } catch (e) {
              toast(errorMessage(e), true);
            } finally {
              setBusy(false);
            }
          }}
        />
      )}
    </>
  );
}
export function CatalogManagement({
  kind,
}: {
  kind: "services" | "categories";
}) {
  const query = useApi<Catalog[]>("/" + kind);
  const toast = useToast();
  const [edit, setEdit] = useState<Catalog | null | undefined>();
  const [form, setForm] = useState({
    name: "",
    active: true,
    criticality: 2,
    requiredSkill: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const noun = kind === "services" ? "IT service" : "category";
  function open(item: Catalog | null) {
    setEdit(item);
    setError("");
    setForm(
      item || { name: "", active: true, criticality: 2, requiredSkill: "" },
    );
  }
  return (
    <>
      <PageHeading
        eyebrow="SERVICE CATALOG"
        title={kind === "services" ? "IT services" : "Incident categories"}
        description={
          kind === "services"
            ? "Define the services your organization depends on."
            : "Organize incidents and connect them to the right expertise."
        }
        action={
          <button className="btn primary" onClick={() => open(null)}>
            <Plus size={17} />
            Add {noun}
          </button>
        }
      />
      {query.loading ? (
        <Loading />
      ) : query.error ? (
        <ErrorState message={query.error} retry={query.reload} />
      ) : (
        <div className="catalog-grid">
          {query.data?.map((c) => (
            <article className="panel catalog-card" key={c.id}>
              <div className="catalog-top">
                <span className="stat-icon blue">
                  {kind === "services" ? (
                    <Layers size={22} />
                  ) : (
                    <Shapes size={22} />
                  )}
                </span>
                <Badge value={c.active ? "ACTIVE" : "INACTIVE"} />
              </div>
              <h2>{c.name}</h2>
              <p>
                Required expertise: <strong>{c.requiredSkill}</strong>
              </p>
              {kind === "services" && (
                <div className="criticality">
                  Business criticality <strong>{c.criticality} / 3</strong>
                </div>
              )}
              <button className="btn secondary full" onClick={() => open(c)}>
                <Edit3 size={15} />
                Edit {noun}
              </button>
            </article>
          ))}
        </div>
      )}
      {edit !== undefined && (
        <Modal
          title={`${edit ? "Edit" : "Add"} ${noun}`}
          onClose={() => setEdit(undefined)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                await (edit
                  ? api.put(`/${kind}/${edit.id}`, form)
                  : api.post("/" + kind, form));
                toast("Catalog saved");
                setEdit(undefined);
                query.reload();
              } catch (e) {
                setError(errorMessage(e));
              } finally {
                setBusy(false);
              }
            }}
          >
            <label>
              Name
              <input
                required
                maxLength={120}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </label>
            <label>
              Required skill
              <input
                required
                maxLength={80}
                value={form.requiredSkill}
                onChange={(e) =>
                  setForm({ ...form, requiredSkill: e.target.value })
                }
                placeholder="For example: Network"
              />
            </label>
            {kind === "services" && (
              <label>
                Business criticality
                <select
                  value={form.criticality}
                  onChange={(e) =>
                    setForm({ ...form, criticality: +e.target.value })
                  }
                >
                  <option value={1}>1 · Standard</option>
                  <option value={2}>2 · Important</option>
                  <option value={3}>3 · Business critical</option>
                </select>
              </label>
            )}
            <label className="checkbox">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
              />
              Active and available for reporting
            </label>
            {error && (
              <p className="inline-error" role="alert">
                {error}
              </p>
            )}
            <div className="form-actions">
              <button
                type="button"
                className="btn secondary"
                onClick={() => setEdit(undefined)}
              >
                Cancel
              </button>
              <button className="btn primary" disabled={busy}>
                {busy ? "Saving…" : "Save " + noun}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
export function SlaPolicies() {
  const query = useApi<Policy[]>("/sla/policies");
  const toast = useToast();
  const [edit, setEdit] = useState<Policy | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return (
    <>
      <PageHeading
        eyebrow="SERVICE COMMITMENTS"
        title="SLA policies"
        description="Set clear response and resolution targets for every priority."
        action={
          <Link className="btn secondary" to="/app/incidents?slaLevel=VIOLATED">
            View SLA violations
            <ArrowUpRight size={16} />
          </Link>
        }
      />
      <div className="info-banner">
        <Clock3 size={20} />
        <div>
          <strong>Deadlines are monitored continuously</strong>
          <p>
            Changes apply to new or reprioritized incidents. Existing deadlines
            remain anchored to their creation time.
          </p>
        </div>
      </div>
      <section className="panel">
        {query.loading ? (
          <Loading />
        ) : query.error ? (
          <ErrorState message={query.error} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Priority</th>
                  <th>Response target</th>
                  <th>Resolution target</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {query.data
                  ?.sort((a, b) => a.priority.localeCompare(b.priority))
                  .map((p) => (
                    <tr key={p.id}>
                      <td>
                        <Badge value={p.priority} type="priority" />
                      </td>
                      <td>{p.responseMinutes} minutes</td>
                      <td>{p.resolutionMinutes} minutes</td>
                      <td>
                        <button
                          className="btn secondary small-btn"
                          onClick={() => {
                            setEdit({ ...p });
                            setError("");
                          }}
                        >
                          <Edit3 size={14} />
                          Edit policy
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <div className="sla-level-grid">
        {[
          ["SAFE", "Within target"],
          ["APPROACHING", "Within 10 minutes"],
          ["AT_RISK", "Within 2 minutes"],
          ["VIOLATED", "A deadline exceeded"],
        ].map(([level, text]) => (
          <div className="panel" key={level}>
            <Badge value={level} type="sla" />
            <p>{text}</p>
          </div>
        ))}
      </div>
      {edit && (
        <Modal
          title={`Configure ${edit.priority} policy`}
          onClose={() => setEdit(null)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                await api.put(`/sla/policies/${edit.id}`, edit);
                toast("SLA policy updated");
                setEdit(null);
                query.reload();
              } catch (e) {
                setError(errorMessage(e));
              } finally {
                setBusy(false);
              }
            }}
          >
            <label>
              Response target (minutes)
              <input
                required
                type="number"
                min={1}
                max={525600}
                value={edit.responseMinutes}
                onChange={(e) =>
                  setEdit({ ...edit, responseMinutes: +e.target.value })
                }
              />
            </label>
            <label>
              Resolution target (minutes)
              <input
                required
                type="number"
                min={edit.responseMinutes}
                max={525600}
                value={edit.resolutionMinutes}
                onChange={(e) =>
                  setEdit({ ...edit, resolutionMinutes: +e.target.value })
                }
              />
            </label>
            {error && (
              <p className="inline-error" role="alert">
                {error}
              </p>
            )}
            <div className="form-actions">
              <button
                type="button"
                className="btn secondary"
                onClick={() => setEdit(null)}
              >
                Cancel
              </button>
              <button className="btn primary" disabled={busy}>
                {busy ? "Saving…" : "Save policy"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
export function Automation() {
  const query = useApi<Settings>("/settings");
  if (query.loading) return <Loading />;
  if (query.error || !query.data) return <ErrorState message={query.error} />;
  return <AutomationForm initial={query.data} />;
}
function AutomationForm({ initial }: { initial: Settings }) {
  const [form, setForm] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const toast = useToast();
  return (
    <>
      <PageHeading
        eyebrow="EXPLAINABLE INTELLIGENCE"
        title="Automation"
        description="Choose how incidents are prioritized and routed to your team."
      />
      <form
        className="settings-form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          try {
            await api.put("/settings", form);
            toast("Automation settings saved");
          } catch (e) {
            setError(errorMessage(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <section className="panel">
          <div className="panel-header">
            <div>
              <h2>
                <Workflow size={18} />
                Technician assignment
              </h2>
              <p>
                Availability and maximum workload are enforced by both
                strategies.
              </p>
            </div>
          </div>
          <div className="panel-body strategy-options">
            {[
              {
                key: "SKILL",
                title: "Skill-based routing",
                text: "Match category and service skills, consider experience, then balance workload.",
              },
              {
                key: "LOAD",
                title: "Lowest workload",
                text: "Route to the available technician with the fewest active incidents.",
              },
            ].map((s) => (
              <label
                className={`strategy-option ${form.assignmentStrategy === s.key ? "selected" : ""}`}
                key={s.key}
              >
                <input
                  type="radio"
                  name="assignment"
                  value={s.key}
                  checked={form.assignmentStrategy === s.key}
                  onChange={() =>
                    setForm({ ...form, assignmentStrategy: s.key })
                  }
                />
                <div>
                  <strong>{s.title}</strong>
                  <p>{s.text}</p>
                </div>
                {form.assignmentStrategy === s.key && <Check size={18} />}
              </label>
            ))}
          </div>
        </section>
        <section className="panel">
          <div className="panel-header">
            <div>
              <h2>
                <Sparkles size={18} />
                Priority calculation
              </h2>
              <p>Explainable scoring, configurable thresholds.</p>
            </div>
          </div>
          <div className="panel-body">
            <label>
              Active priority strategy
              <select
                value={form.priorityStrategy}
                onChange={(e) =>
                  setForm({ ...form, priorityStrategy: e.target.value })
                }
              >
                <option value="WEIGHTED">
                  Weighted · impact × urgency × criticality × user factor
                </option>
                <option value="RULE">
                  Rule-based · weighted addition with user bands
                </option>
              </select>
            </label>
            <p className="muted">
              Weighted user factor: 1 for 1–9 people, 1.2 for 10–99, 1.5 for
              100+. Rule-based: impact × 3 + urgency × 3 + criticality × 2 +
              user band (0, 3, 6).
            </p>
            <div className="form-grid three">
              {(["p1Threshold", "p2Threshold", "p3Threshold"] as const).map(
                (key, n) => (
                  <label key={key}>
                    P{n + 1} minimum score
                    <input
                      required
                      type="number"
                      min={0.1}
                      max={1000}
                      step={0.1}
                      value={form[key]}
                      onChange={(e) =>
                        setForm({ ...form, [key]: +e.target.value })
                      }
                    />
                  </label>
                ),
              )}
            </div>
            <small className="muted">
              P1 &gt; P2 &gt; P3 &gt; 0. Below P3 is low priority (P4). Changes
              affect new or edited incidents.
            </small>
          </div>
        </section>
        <section className="info-banner">
          <ShieldCheck size={20} />
          <div>
            <strong>Duplicate detection stays active</strong>
            <p>
              Recent incident text, service, and category are compared.
              Suggestions at or above 62% similarity require a human review.
            </p>
          </div>
        </section>
        {error && (
          <div className="inline-error" role="alert">
            {error}
          </div>
        )}
        <div className="form-actions">
          <button className="btn primary" disabled={busy}>
            {busy ? "Saving…" : "Save automation settings"}
          </button>
        </div>
      </form>
    </>
  );
}
export function Workload() {
  const query = useApi<Metric[]>("/reports/technician-workload");
  return (
    <>
      <PageHeading
        eyebrow="TEAM CAPACITY"
        title="Workload overview"
        description="A live view of active work and available capacity."
      />
      {query.loading ? (
        <Loading />
      ) : query.error ? (
        <ErrorState message={query.error} />
      ) : (
        <div className="catalog-grid">
          {query.data?.map((t) => (
            <article className="panel workload-card" key={t.id}>
              <div className="catalog-top">
                <span className="avatar">{initials(t.name)}</span>
                <Badge value={t.available ? "AVAILABLE" : "UNAVAILABLE"} />
              </div>
              <h2>{t.name}</h2>
              <div className="capacity-value">
                <strong>{t.value}</strong>
                <span>/ {t.capacity} active incidents</span>
              </div>
              <div className="capacity-bar">
                <span
                  style={{
                    width: `${Math.min(100, (t.value / (t.capacity || 1)) * 100)}%`,
                    background:
                      t.value >= (t.capacity || 1) ? "#df7a72" : "#5b7ddd",
                  }}
                />
              </div>
              <p>{Math.max(0, (t.capacity || 0) - t.value)} slots remaining</p>
              <Link
                className="btn secondary full"
                to={`/app/incidents?technicianId=${t.id}`}
              >
                View assigned incidents
                <ArrowUpRight size={15} />
              </Link>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
export function AuditTrail() {
  const [page, setPage] = useState(0);
  const query = useApi<Page<Audit>>(`/audit-logs?page=${page}`);
  return (
    <>
      <PageHeading
        eyebrow="ACCOUNTABILITY, BUILT IN"
        title="Audit trail"
        description="A permanent record of incident and administrative activity."
      />
      <section className="panel">
        {query.loading ? (
          <Loading />
        ) : query.error ? (
          <ErrorState message={query.error} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Event</th>
                  <th>Incident</th>
                  <th>Actor</th>
                  <th>Description</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {query.data?.content.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <span className="audit-action">{label(a.action)}</span>
                    </td>
                    <td>
                      {a.incidentId ? (
                        <Link
                          className="text-link"
                          to={`/app/incidents/${a.incidentId}`}
                        >
                          INC-{a.incidentId}
                        </Link>
                      ) : (
                        "System"
                      )}
                    </td>
                    <td>{a.actor}</td>
                    <td className="audit-description">{a.description}</td>
                    <td className="nowrap">{date(a.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="pagination">
          <span>
            Page {page + 1} · {query.data?.totalElements || 0} events
          </span>
          <div>
            <button
              className="btn secondary"
              disabled={!page}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>
            <button
              className="btn secondary"
              disabled={!query.data || page + 1 >= query.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </section>
    </>
  );
}
