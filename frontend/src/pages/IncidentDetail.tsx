import { IncidentChat } from "../components/IncidentChat";
import { useState, type FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Clock3,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  UserRound,
  Edit3,
  Star,
  Plus,
  CheckCircle2,
} from "lucide-react";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, errorMessage } from "../services/api";
import type { Detail, User } from "../types";
import {
  Badge,
  PageHeading,
  Loading,
  ErrorState,
  Modal,
  Confirm,
} from "../components/UI";
import { date, label, initials, remaining } from "../utils/format";
import { IncidentForm } from "./Incidents";
interface Recommendation {
  technicianId: number;
  technicianName: string;
  reason: string;
  score: number;
}
export function IncidentDetail() {
  const { id } = useParams();
  const path = `/incidents/${id}`;
  const query = useApi<Detail>(path);
  const { user } = useAuth();
  const toast = useToast();
  const [modal, setModal] = useState("");
  const [details, setDetails] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const [techId, setTechId] = useState("");
  const [rating, setRating] = useState(5);
  const technicians = useApi<User[]>(
    user?.role === "ADMIN" ? "/technicians" : null,
  );
  const recommendation = useApi<Recommendation[]>(
    user?.role === "ADMIN" ? path + "/recommendations" : null,
  );
  if (query.loading && !query.data) return <Loading />;
  if (query.error || !query.data)
    return <ErrorState message={query.error} retry={query.reload} />;
  const { incident: i, notes, history, duplicates, feedback } = query.data;
  const isReporter = user!.id === i.reporter.id;
  const canChat = Boolean(i.technician && (isReporter || user!.id === i.technician.id));
  const requestedTab = searchParams.get("tab");
  const tab = requestedTab === "notes" || requestedTab === "history"
      || (requestedTab === "duplicates" && user!.role !== "EMPLOYEE")
    ? requestedTab
    : canChat ? "chat" : "notes";
  function setTab(nextTab: string) {
    setSearchParams((previous) => {
      const params = new URLSearchParams(previous);
      params.set("tab", nextTab);
      return params;
    }, { replace: true });
  }
  const canWork =
    user!.role === "ADMIN" ||
    (user!.role === "TECHNICIAN" && user!.id === i.technician?.id);
  const active = !["RESOLVED", "VERIFIED", "CLOSED"].includes(i.status);
  const workflow = [
    "NEW",
    "CLASSIFIED",
    "ASSIGNED",
    "ACKNOWLEDGED",
    "IN_PROGRESS",
    "RESOLVED",
    "VERIFIED",
    "CLOSED",
  ];
  const open = (action: string) => {
    setModal(action);
    setDetails("");
    setError("");
    setTechId("");
  };
  async function perform(action: string, body: unknown = {}) {
    setBusy(true);
    setError("");
    try {
      await api.post(`${path}/${action}`, body);
      toast("Incident updated");
      setModal("");
      setDetails("");
      await query.reload();
    } catch (e) {
      setError(errorMessage(e));
      toast(errorMessage(e), true);
    } finally {
      setBusy(false);
    }
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    await perform(
      modal,
      modal === "assign"
        ? { technicianId: +techId, reason: details }
        : modal === "feedback"
          ? { rating, comment: details }
          : { details },
    );
  }
  const immediate =
    modal === "acknowledge" || modal === "investigate" || modal === "close";
  return (
    <>
      <Link to="/app/incidents" className="back-link">
        <ArrowLeft size={15} />
        Back to incidents
      </Link>
      <div className="detail-heading">
        <div className="incident-kicker">
          INC-{String(i.id).padStart(4, "0")}
          <Badge value={i.priority} type="priority" />
          <Badge value={i.status} />
        </div>
        <h1>{i.title}</h1>
        <div className="detail-meta">
          <span>Reported by {i.reporter.name}</span>
          <span>·</span>
          <span>{date(i.createdAt)}</span>
        </div>
      </div>
      <div className="lifecycle-bar" aria-label="Incident lifecycle">
        {workflow.map((s, index) => (
          <div
            key={s}
            className={`${index < workflow.indexOf(i.status) ? "complete" : ""} ${s === i.status ? "current" : ""}`}
          >
            <span>
              {index < workflow.indexOf(i.status) ? (
                <Check size={12} />
              ) : (
                index + 1
              )}
            </span>
            <small>{label(s)}</small>
          </div>
        ))}
      </div>
      <div className="incident-workspace">
        <div className="incident-main">
          <section className="panel">
            <div className="panel-header">
              <h2>Incident overview</h2>
              {active &&
                (user!.role === "ADMIN" ||
                  (isReporter &&
                    ["CLASSIFIED", "ASSIGNED"].includes(i.status))) && (
                  <button
                    className="btn secondary small-btn"
                    onClick={() => open("edit")}
                  >
                    <Edit3 size={14} />
                    Edit
                  </button>
                )}
            </div>
            <div className="panel-body">
              <p className="pre-wrap description">{i.description}</p>
              <div className="detail-properties">
                <div>
                  <span>Service</span>
                  <strong>{i.service.name}</strong>
                </div>
                <div>
                  <span>Category</span>
                  <strong>{i.category.name}</strong>
                </div>
                <div>
                  <span>Impact / urgency</span>
                  <strong>
                    {i.impact} / {i.urgency}
                  </strong>
                </div>
                <div>
                  <span>Affected users</span>
                  <strong>{i.affectedUsers.toLocaleString()}</strong>
                </div>
              </div>
            </div>
          </section>
          <section className="intelligence-card">
            <div className="intelligence-title">
              <Sparkles size={17} />
              <h3>Intelligence, explained</h3>
              <span>Score {i.priorityScore}</span>
            </div>
            <p>{i.priorityReason}</p>
            {i.assignmentReason && (
              <>
                <div className="intelligence-divider" />
                <strong>Technician recommendation</strong>
                <p>{i.assignmentReason}</p>
              </>
            )}
          </section>
          {i.resolutionDetails && (
            <section className="panel resolution-panel">
              <div className="panel-header">
                <h2>
                  <CheckCircle2 size={18} />
                  Resolution details
                </h2>
                <span>{date(i.resolvedAt)}</span>
              </div>
              <div className="panel-body">
                <p className="pre-wrap">{i.resolutionDetails}</p>
                {i.verificationDetails && (
                  <div className="verification-note">
                    <ShieldCheck size={17} />
                    <div>
                      <strong>Verified by reporter</strong>
                      <p>{i.verificationDetails}</p>
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}
          <section className="panel activity-panel">
            <div className="tabs">
              {canChat &&
                <button className={tab === "chat" ? "active" : ""} onClick={() => setTab("chat")}>Chat</button>}
              <button
                className={tab === "notes" ? "active" : ""}
                onClick={() => setTab("notes")}
              >
                Incident notes <span>{notes.length}</span>
              </button>
              <button
                className={tab === "history" ? "active" : ""}
                onClick={() => setTab("history")}
              >
                Audit history <span>{history.length}</span>
              </button>
              {user!.role !== "EMPLOYEE" && (
                <button
                  className={tab === "duplicates" ? "active" : ""}
                  onClick={() => setTab("duplicates")}
                >
                  Related incidents <span>{duplicates.length}</span>
                </button>
              )}
            </div>
            {tab === "chat" && canChat && i.technician &&
              <IncidentChat
                key={i.id + "-" + i.technician.id}
                incidentId={i.id}
                technicianId={i.technician.id}
                participantName={isReporter ? i.technician.name : i.reporter.name}
                userId={user!.id}
                canReply={i.status !== "CLOSED"}
              />}
            {tab === "notes" && (
              <div className="panel-body">
                {notes.length ? (
                  notes.map((n) => (
                    <article key={n.id} className="note">
                      <span className="avatar">{initials(n.author.name)}</span>
                      <div>
                        <div className="note-heading">
                          <strong>{n.author.name}</strong>
                          <small>{date(n.createdAt)}</small>
                        </div>
                        {n.informationRequest && (
                          <span className="information-label">
                            Information requested
                          </span>
                        )}
                        <p className="pre-wrap">{n.content}</p>
                      </div>
                    </article>
                  ))
                ) : (
                  <div className="conversation-empty">
                    <MessageSquare size={22} />
                    <p>
                      No updates yet. Add context or share progress with your
                      team.
                    </p>
                  </div>
                )}
                {i.status !== "CLOSED" && (
                  <form
                    onSubmit={async (e) => {
                      e.preventDefault();
                      await perform("notes", { details });
                    }}
                    className="note-form"
                  >
                    <label htmlFor="note">Add an update</label>
                    <textarea
                      id="note"
                      required
                      minLength={3}
                      maxLength={10000}
                      rows={3}
                      placeholder="Share additional details or an investigation update…"
                      value={details}
                      onChange={(e) => setDetails(e.target.value)}
                    />
                    <button className="btn primary" disabled={busy}>
                      Post update <ArrowRight size={15} />
                    </button>
                  </form>
                )}
              </div>
            )}
            {tab === "history" && (
              <div className="audit-timeline">
                {history.map((h) => (
                  <article key={h.id}>
                    <span className="timeline-dot" />
                    <div>
                      <strong>{label(h.action)}</strong>
                      <small>
                        {h.actor} · {date(h.createdAt)}
                      </small>
                      <p>{h.description}</p>
                      {h.previousState &&
                        h.newState &&
                        h.previousState !== h.newState && (
                          <div className="history-transition">
                            {label(h.previousState)}
                            <ArrowRight size={12} />
                            {label(h.newState)}
                          </div>
                        )}
                    </div>
                  </article>
                ))}
              </div>
            )}
            {tab === "duplicates" && (
              <div className="panel-body">
                <p className="muted">
                  Suggestions combine text similarity (60%), service (25%), and
                  category (15%). Review suggestions without removing either
                  record.
                </p>
                {duplicates.length ? (
                  duplicates.map((d) => (
                    <article key={d.id} className="duplicate-card">
                      <div>
                        <span className="incident-ref">
                          INC-{String(d.relatedId).padStart(4, "0")} ·{" "}
                          {Math.round(d.score * 100)}% similarity
                        </span>
                        <strong>{d.relatedTitle}</strong>
                      </div>
                      <Badge value={d.decision} />
                      {canWork && d.decision === "PENDING" && (
                        <div className="duplicate-actions">
                          <button
                            className="btn secondary small-btn"
                            disabled={busy}
                            onClick={() =>
                              perform(`duplicates/${d.id}`, {
                                decision: "REJECTED",
                              })
                            }
                          >
                            Reject
                          </button>
                          <button
                            className="btn primary small-btn"
                            disabled={busy}
                            onClick={() =>
                              perform(`duplicates/${d.id}`, {
                                decision: "CONFIRMED",
                              })
                            }
                          >
                            Confirm duplicate
                          </button>
                        </div>
                      )}
                    </article>
                  ))
                ) : (
                  <div className="conversation-empty">
                    No related incidents found.
                  </div>
                )}
              </div>
            )}
          </section>
        </div>
        <aside className="incident-side">
          <section className="panel">
            <div className="panel-header">
              <h2>Next steps</h2>
            </div>
            <div className="panel-body action-stack">
              {canWork && i.status === "ASSIGNED" && (
                <button
                  className="btn primary full"
                  onClick={() => open("acknowledge")}
                >
                  Acknowledge incident
                  <Check size={16} />
                </button>
              )}
              {canWork && i.status === "ACKNOWLEDGED" && (
                <button
                  className="btn primary full"
                  onClick={() => open("investigate")}
                >
                  Start investigation
                  <ArrowRight size={16} />
                </button>
              )}
              {canWork && i.status === "IN_PROGRESS" && (
                <button
                  className="btn primary full"
                  onClick={() => open("resolve")}
                >
                  Record resolution
                  <CheckCircle2 size={16} />
                </button>
              )}
              {isReporter && i.status === "RESOLVED" && (
                <button
                  className="btn primary full"
                  onClick={() => open("verify")}
                >
                  Verify resolution
                  <ShieldCheck size={16} />
                </button>
              )}
              {isReporter && i.status === "VERIFIED" && (
                <button
                  className="btn primary full"
                  onClick={() => open("close")}
                >
                  Close incident
                  <CheckCircle2 size={16} />
                </button>
              )}
              {canWork && active && (
                <button
                  className="btn secondary full"
                  onClick={() => open("request-information")}
                >
                  Request information
                </button>
              )}
              {user!.role === "ADMIN" && active && (
                <button
                  className="btn secondary full"
                  onClick={() => open("assign")}
                >
                  {i.technician ? "Reassign technician" : "Assign technician"}
                </button>
              )}
              {(isReporter || canWork) &&
                ["RESOLVED", "VERIFIED", "CLOSED"].includes(i.status) && (
                  <button
                    className="btn secondary full"
                    onClick={() => open("reopen")}
                  >
                    Reopen with a reason
                  </button>
                )}
              {isReporter &&
                ["VERIFIED", "CLOSED"].includes(i.status) &&
                !feedback.rating && (
                  <button
                    className="btn secondary full"
                    onClick={() => open("feedback")}
                  >
                    <Star size={16} />
                    Leave feedback
                  </button>
                )}
              {feedback.rating && (
                <div className="feedback-rating">
                  {"★".repeat(feedback.rating)}
                  {"☆".repeat(5 - feedback.rating)}
                  <p>{feedback.comment || "Thank you for your feedback."}</p>
                </div>
              )}
              <p className="action-hint">
                {i.status === "CLOSED"
                  ? "This incident is closed and its history is preserved."
                  : isReporter && active
                    ? "Your support team is working through the incident lifecycle. You can add details below."
                    : "Every action is validated and recorded in the audit history."}
              </p>
            </div>
          </section>
          <section className="panel">
            <div className="panel-header">
              <h2>
                <Clock3 size={17} />
                Service level
              </h2>
              <Badge value={i.slaLevel} type="sla" />
            </div>
            <div className="panel-body sla-details">
              <div>
                <span>Response deadline</span>
                <strong>{date(i.responseDeadline)}</strong>
                <small className={i.responseViolated ? "danger" : ""}>
                  {i.acknowledgedAt
                    ? "Acknowledged " + date(i.acknowledgedAt)
                    : remaining(i.responseDeadline)}
                </small>
              </div>
              <div>
                <span>Resolution deadline</span>
                <strong>{date(i.resolutionDeadline)}</strong>
                <small className={i.resolutionViolated ? "danger" : ""}>
                  {remaining(i.resolutionDeadline, !!i.resolvedAt)}
                </small>
              </div>
              <p className="sla-footnote">
                Monitored on the server every 30 seconds. Times displayed in
                your local timezone.
              </p>
            </div>
          </section>
          <section className="panel">
            <div className="panel-header">
              <h2>People</h2>
              <UserRound size={16} />
            </div>
            <div className="panel-body people-details">
              <span className="field-caption">ASSIGNED TECHNICIAN</span>
              {i.technician ? (
                <div className="person-cell">
                  <span className="avatar">{initials(i.technician.name)}</span>
                  <div>
                    <strong>{i.technician.name}</strong>
                    <small>{i.technician.email}</small>
                  </div>
                </div>
              ) : (
                <p className="muted">Awaiting an available technician</p>
              )}
              <span className="field-caption">REPORTED BY</span>
              <div className="person-cell">
                <span className="avatar employee-avatar">
                  {initials(i.reporter.name)}
                </span>
                <div>
                  <strong>{i.reporter.name}</strong>
                  <small>{i.reporter.email}</small>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>
      {modal === "edit" && (
        <Modal title="Edit incident" onClose={() => setModal("")}>
          <IncidentForm
            existing={i}
            onSaved={() => {
              setModal("");
              query.reload();
            }}
          />
        </Modal>
      )}
      {immediate && (
        <Confirm
          title={label(modal) + " incident"}
          text={`This will ${modal} INC-${i.id} and notify the relevant people.`}
          onClose={() => setModal("")}
          busy={busy}
          onConfirm={() => perform(modal)}
        />
      )}{" "}
      {modal && !immediate && modal !== "edit" && (
        <Modal
          title={label(modal.replaceAll("-", " "))}
          onClose={() => setModal("")}
        >
          <form onSubmit={submit}>
            {modal === "assign" && (
              <>
                <label>
                  Technician
                  <select
                    required
                    value={techId}
                    onChange={(e) => setTechId(e.target.value)}
                  >
                    <option value="">Choose a technician</option>
                    {Array.isArray(technicians.data) &&
                      technicians.data
                        .filter((t) => t.active && t.available)
                        .map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.name} · capacity {t.maxWorkload}
                          </option>
                        ))}
                  </select>
                </label>
                {Array.isArray(recommendation.data) &&
                  recommendation.data[0]?.technicianId && (
                    <div className="recommendation-box">
                      <Sparkles size={17} />
                      <div>
                        <strong>
                          Recommended: {recommendation.data[0].technicianName}
                        </strong>
                        <p>{recommendation.data[0].reason}</p>
                        <button
                          type="button"
                          className="text-link"
                          onClick={() => {
                            const r = recommendation.data![0];
                            setTechId(String(r.technicianId));
                            setDetails(r.reason);
                          }}
                        >
                          Use recommendation
                        </button>
                      </div>
                    </div>
                  )}
              </>
            )}
            {modal === "feedback" && (
              <label>
                Rating
                <select
                  value={rating}
                  onChange={(e) => setRating(+e.target.value)}
                >
                  {[5, 4, 3, 2, 1].map((n) => (
                    <option key={n} value={n}>
                      {n} / 5 ·{" "}
                      {n >= 4
                        ? "Great"
                        : n === 3
                          ? "Okay"
                          : "Needs improvement"}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label>
              {modal === "feedback"
                ? "Your feedback (optional)"
                : modal === "assign"
                  ? "Assignment reason"
                  : modal === "resolve"
                    ? "Resolution details"
                    : modal === "verify"
                      ? "How did you confirm the fix?"
                      : modal === "reopen"
                        ? "Reason for reopening"
                        : "Message"}
              <textarea
                required={modal !== "feedback"}
                minLength={["resolve", "reopen"].includes(modal) ? 10 : 3}
                maxLength={
                  modal === "assign" ? 255 : modal === "feedback" ? 2000 : 10000
                }
                rows={5}
                value={details}
                onChange={(e) => setDetails(e.target.value)}
                placeholder={
                  modal === "resolve"
                    ? "Describe the root cause, the fix, and how it was tested."
                    : modal === "reopen"
                      ? "Explain what is still not working."
                      : "Add helpful details…"
                }
              />
            </label>
            {error && (
              <div className="inline-error" role="alert">
                {error}
              </div>
            )}
            <div className="form-actions">
              <button
                className="btn secondary"
                type="button"
                onClick={() => setModal("")}
              >
                Cancel
              </button>
              <button className="btn primary" disabled={busy}>
                {busy ? "Saving…" : "Save & notify"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
