import { exportIncidents } from "../utils/exportIncidents";
import { useState, useEffect, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  Plus,
  Search,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  X,
  ArrowLeft,
  ShieldCheck,
} from "lucide-react";
import { useApi } from "../hooks/useApi";
import { useToast } from "../context/ToastContext";
import { api, errorMessage } from "../services/api";
import { useAuth } from "../context/AuthContext";
import type { Page, Incident, Catalog, User, IncidentInput } from "../types";
import { PageHeading, Loading, ErrorState } from "../components/UI";
import { IncidentTable } from "../components/IncidentTable";
import { label } from "../utils/format";
export function Incidents() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const [exporting, setExporting] = useState(false);
  const toast = useToast();
  const [query, setQuery] = useState(params.get("q") || "");
  const services = useApi<Catalog[]>("/services");
  const categories = useApi<Catalog[]>("/categories");
  const technicians = useApi<User[]>(
    user?.role === "ADMIN" ? "/technicians" : null,
  );
  const page = Number(params.get("page") || 0);
  const filter = (key: string, value: string) => {
    setParams((p) => {
      const next = new URLSearchParams(p);
      if (value) next.set(key, value);
      else next.delete(key);
      if (key !== "page") next.delete("page");
      return next;
    });
  };
  const incidents = useApi<Page<Incident>>(
    `/incidents?${params.toString()}&size=15`,
  );
  useEffect(() => {
    const timer = setTimeout(() => {
      if (query !== (params.get("q") || "")) filter("q", query);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);
  async function download(format: "csv" | "xlsx") {
    setExporting(true);
    const snapshot = new URLSearchParams(params);
    if (query) snapshot.set("q", query); else snapshot.delete("q");
    try { await exportIncidents(format, snapshot); }
    catch (e) { toast(errorMessage(e), true); }
    finally { setExporting(false); }
  }
  function select(key: string, title: string, options: [string,string][]) {
    return <select aria-label={"Filter by " + title} value={params.get(key) || ""}
      onChange={e=>filter(key,e.target.value)}><option value="">All</option>
      {options.map(([value,name])=><option key={value} value={value}>{name}</option>)}</select>;
  }
  const options = (values: string[]): [string,string][] => values.map(value => [value,label(value)]);
  const headers = {
    Incident: <input aria-label="Search incidents" placeholder="ID or text" value={query} onChange={e=>setQuery(e.target.value)}/>,
    Priority: select("priority","priority", options(["P1","P2","P3","P4"])),
    Status: select("status","status", options(["NEW","CLASSIFIED","ASSIGNED","ACKNOWLEDGED","IN_PROGRESS","RESOLVED","VERIFIED","CLOSED"])),
    "Affected service": select("serviceId","service",(services.data || []).map(s=>[String(s.id),s.name])),
    Created: <div>{["from","to"].map(key=><label key={key}>{key === "from" ? "From" : "To"} (UTC)<input type="date"
      value={params.get(key) || ""} onChange={e=>filter(key,e.target.value)}/></label>)}</div>,
    "Assigned to": user?.role === "ADMIN" ? select("technicianId","technician",(technicians.data || []).map(t=>[String(t.id),t.name]))
      : <input aria-label="Filter by technician name" placeholder="Name" value={params.get("technicianName") || ""} onChange={e=>filter("technicianName",e.target.value)}/>,
    "SLA deadline": select("slaLevel","SLA status",options(["SAFE","APPROACHING","AT_RISK","VIOLATED"])),
  };
  return (
    <>
      <PageHeading
        eyebrow="INCIDENT MANAGEMENT"
        title={
          user?.role === "EMPLOYEE"
            ? "My incidents"
            : user?.role === "TECHNICIAN"
              ? "Assigned incidents"
              : "All incidents"
        }
        description="Clear priorities. Visible progress. Every incident in one place."
        action={
          <Link className="btn primary" to="/app/incidents/new">
            <Plus size={17} />
            Report incident
          </Link>
        }
      />
      <section className="panel">
        <div className="filter-toolbar">
          <label>Category{select("categoryId","category",(categories.data || []).map(c=>[String(c.id),c.name]))}</label>
          <button className="btn secondary" disabled={exporting} onClick={()=>download("xlsx")}>Export Excel</button>
          <button className="btn secondary" disabled={exporting} onClick={()=>download("csv")}>Export CSV</button>
        </div>
        {params.size > 0 && (
          <div className="active-filters">
            <span>
              {incidents.data?.totalElements ?? "…"} matching incidents
            </span>
            <button
              onClick={() => {
                setParams({});
                setQuery("");
              }}
            >
              <X size={13} />
              Clear filters
            </button>
          </div>
        )}
        {incidents.error ? (
          <ErrorState message={incidents.error} retry={incidents.reload} />
        ) : (
          <div aria-busy={incidents.loading}><IncidentTable incidents={incidents.data?.content || []} filters={headers}/></div>
        )}
        <div className="pagination">
          <span>
            {incidents.data?.totalElements || 0} incidents · Page {page + 1} of{" "}
            {Math.max(1, incidents.data?.totalPages || 1)}
          </span>
          <div>
            <button
              className="btn secondary"
              disabled={page === 0 || incidents.loading}
              onClick={() => filter("page", String(page - 1))}
            >
              <ChevronLeft size={15} />
              Previous
            </button>
            <button
              className="btn secondary"
              disabled={
                !incidents.data ||
                page + 1 >= incidents.data.totalPages ||
                incidents.loading
              }
              onClick={() => filter("page", String(page + 1))}
            >
              Next
              <ChevronRight size={15} />
            </button>
          </div>
        </div>
      </section>
    </>
  );
}
export function IncidentForm({
  existing,
  onSaved,
}: {
  existing?: Incident;
  onSaved?: () => void;
}) {
  const navigate = useNavigate();
  const toast = useToast();
  const services = useApi<Catalog[]>("/services");
  const categories = useApi<Catalog[]>("/categories");
  const [form, setForm] = useState<IncidentInput>({
    title: existing?.title || "",
    description: existing?.description || "",
    serviceId: existing?.service.id || 0,
    categoryId: existing?.category.id || 0,
    impact: existing?.impact || 2,
    urgency: existing?.urgency || 2,
    affectedUsers: existing?.affectedUsers || 1,
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  function set(key: keyof IncidentInput, value: string | number) {
    setForm((f) => ({ ...f, [key]: value }));
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const i = (
        await (existing
          ? api.put<Incident>(`/incidents/${existing.id}`, form)
          : api.post<Incident>("/incidents", form))
      ).data;
      toast(
        existing
          ? "Incident updated"
          : "Incident reported. Classification and assignment are complete.",
      );
      if (onSaved) onSaved();
      else navigate(`/app/incidents/${i.id}`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  if (services.loading || categories.loading) return <Loading />;
  if (services.error || categories.error)
    return <ErrorState message={services.error || categories.error} />;
  return (
    <form onSubmit={submit} className="incident-form">
      <div className="form-section">
        <div className="section-heading">
          <span>01</span>
          <div>
            <h2>The essentials</h2>
            <p>A little context helps us get you to the right person.</p>
          </div>
        </div>
        <label>
          Incident title <span className="required">*</span>
          <input
            autoFocus
            required
            minLength={5}
            maxLength={180}
            value={form.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="A short summary of what’s happening"
          />
        </label>
        <div className="form-grid">
          <label>
            Affected IT service <span className="required">*</span>
            <select
              required
              value={form.serviceId || ""}
              onChange={(e) => set("serviceId", +e.target.value)}
            >
              <option value="" disabled>
                Select a service
              </option>
              {services.data
                ?.filter((s) => s.active)
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
            </select>
          </label>
          <label>
            Category <span className="required">*</span>
            <select
              required
              value={form.categoryId || ""}
              onChange={(e) => set("categoryId", +e.target.value)}
            >
              <option value="" disabled>
                Select a category
              </option>
              {categories.data
                ?.filter((c) => c.active)
                .map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </select>
          </label>
        </div>
        <label>
          What went wrong? <span className="required">*</span>
          <textarea
            required
            minLength={15}
            maxLength={10000}
            rows={5}
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            placeholder="Describe the issue, when it started, and any steps you’ve already tried."
          />
        </label>
        <small className="muted">
          Include error messages and helpful details. Don’t include passwords or
          sensitive information.
        </small>
      </div>
      <div className="form-section">
        <div className="section-heading">
          <span>02</span>
          <div>
            <h2>Help us understand the impact</h2>
            <p>We’ll calculate a priority based on this information.</p>
          </div>
        </div>
        <div className="form-grid three">
          <label>
            Business impact
            <select
              value={form.impact}
              onChange={(e) => set("impact", +e.target.value)}
            >
              <option value={1}>1 · Low — individual task</option>
              <option value={2}>2 · Medium — team affected</option>
              <option value={3}>3 · High — business disrupted</option>
            </select>
          </label>
          <label>
            Urgency
            <select
              value={form.urgency}
              onChange={(e) => set("urgency", +e.target.value)}
            >
              <option value={1}>1 · Low — workaround exists</option>
              <option value={2}>2 · Medium — work delayed</option>
              <option value={3}>3 · High — work blocked</option>
            </select>
          </label>
          <label>
            Affected users
            <input
              required
              type="number"
              min={1}
              max={1000000}
              value={form.affectedUsers}
              onChange={(e) => set("affectedUsers", +e.target.value)}
            />
          </label>
        </div>
      </div>
      {error && (
        <div role="alert" className="inline-error">
          {error}
        </div>
      )}
      <div className="form-actions">
        <button
          type="button"
          className="btn secondary"
          onClick={() => (onSaved ? onSaved() : navigate("/app/incidents"))}
        >
          Cancel
        </button>
        <button className="btn primary" disabled={busy}>
          {busy
            ? "Saving incident…"
            : existing
              ? "Save changes"
              : "Submit incident"}
        </button>
      </div>
    </form>
  );
}
export function CreateIncident() {
  return (
    <>
      <Link to="/app/incidents" className="back-link">
        <ArrowLeft size={15} />
        Back to incidents
      </Link>
      <PageHeading
        eyebrow="WE’RE HERE TO HELP"
        title="Report an incident"
        description="Tell us what’s happening. We’ll take it from here."
      />
      <div className="create-layout">
        <section className="panel">
          <IncidentForm />
        </section>
        <aside className="help-panel">
          <span className="stat-icon blue">
            <ShieldCheck size={22} />
          </span>
          <h3>A clear path to resolution</h3>
          <p>Once you submit your report, the system will:</p>
          <ol>
            <li>Calculate a priority and SLA deadline</li>
            <li>Check for related incidents</li>
            <li>Recommend and assign an available technician</li>
            <li>Keep you updated at every step</li>
          </ol>
          <div className="help-note">
            You can add more details and track progress from your incident
            workspace.
          </div>
        </aside>
      </div>
    </>
  );
}
