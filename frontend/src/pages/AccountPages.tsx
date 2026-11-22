import { useState, useEffect, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  Bell,
  CheckCheck,
  MessageSquare,
  ArrowUpRight,
  Mail,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { IncidentChat } from "../components/IncidentChat";
import { useApi } from "../hooks/useApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, errorMessage } from "../services/api";
import {
  PageHeading,
  Loading,
  ErrorState,
  Empty,
  Badge,
} from "../components/UI";
import type { Notification, Settings, Page } from "../types";
import { label, date, initials } from "../utils/format";
export function Notifications() {
  const query = useApi<Notification[]>("/notifications");
  const { user } = useAuth();
  const [openConversation, setOpenConversation] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const unread = query.data?.filter((n) => !n.read) || [];
  useEffect(() => {
    const timer = setInterval(query.reload, 30000);
    return () => clearInterval(timer);
  }, [query.reload]);
  async function read(ids: number[]) {
    setBusy(true);
    try {
      await Promise.all(
        ids.map((id) => api.patch(`/notifications/${id}/read`)),
      );
      query.reload();
      toast("Notifications marked as read");
    } catch (e) {
      toast(errorMessage(e), true);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="STAY IN THE LOOP"
        title="Notifications"
        description={`${unread.length} unread updates from your service workspace.`}
        action={
          <button
            className="btn secondary"
            disabled={!unread.length || busy}
            onClick={() => read(unread.map((n) => n.id))}
          >
            <CheckCheck size={17} />
            Mark all as read
          </button>
        }
      />
      <section className="panel">
        {query.loading && !query.data ? (
          <Loading />
        ) : query.error ? (
          <ErrorState message={query.error} />
        ) : query.data?.length ? (
          <div className="notifications-list">
            {query.data.map((n) => {
              const conversation = n.conversation;
              const conversationKey = conversation ? `${n.id}-${conversation.technicianId}` : null;
              const expanded = conversationKey !== null && openConversation === conversationKey;
              return (
                <article
                  key={n.id}
                  className={`notification-row ${n.read ? "" : "unread"}`}
                >
                  <div className="notification-icon">
                    {n.eventType === "CHAT_MESSAGE" ? <MessageSquare size={18} /> : <Bell size={18} />}
                  </div>
                  <div className="notification-content">
                    <div>
                      <strong>{label(n.eventType)}</strong>
                      <small>{date(n.createdAt)}</small>
                    </div>
                    <p>{n.message}</p>
                    <div className="notification-links">
                      {n.incidentId && (
                        <Link
                          to={`/app/incidents/${n.incidentId}`}
                          className="text-link"
                        >
                          Open incident
                          <ArrowUpRight size={13} />
                        </Link>
                      )}
                      {conversationKey && (
                        <button
                          className="text-link"
                          aria-expanded={expanded}
                          onClick={() => setOpenConversation(expanded ? null : conversationKey)}
                        >
                          {expanded ? "Hide chat" : conversation?.canReply ? "Reply" : "View chat"}
                        </button>
                      )}
                      {!n.read && (
                        <button
                          className="text-link"
                          disabled={busy}
                          onClick={() => read([n.id])}
                        >
                          Mark as read
                        </button>
                      )}
                    </div>
                    {expanded && conversation && n.incidentId && user && (
                      <div className="notification-chat">
                        <IncidentChat
                          key={conversationKey}
                          incidentId={n.incidentId}
                          technicianId={conversation.technicianId}
                          participantName={conversation.participantName}
                          canReply={conversation.canReply}
                          userId={user.id}
                          notificationId={n.id}
                          onSent={query.reload}
                        />
                      </div>
                    )}
                  </div>
                  {!n.read && <span className="unread-dot" />}
                </article>
              );
            })}
          </div>
        ) : (
          <Empty
            title="You’re all caught up"
            text="Assignment, resolution, and SLA updates will appear here."
          />
        )}
      </section>
    </>
  );
}
export function Profile() {
  const { user, refresh } = useAuth();
  const [name, setName] = useState(user!.name);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const toast = useToast();
  async function save(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api.put("/users/me", { name });
      await refresh();
      toast("Profile updated");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="YOUR ACCOUNT"
        title="Profile"
        description="Your identity, role, and expertise in the workspace."
      />
      <div className="profile-layout">
        <section className="panel profile-card">
          <span className="avatar jumbo">{initials(user!.name)}</span>
          <h2>{user!.name}</h2>
          <p>{user!.email}</p>
          <Badge value={user!.role} />
          <div className="profile-security">
            <ShieldCheck size={18} />
            <span>Role permissions are managed by your administrator.</span>
          </div>
        </section>
        <section className="panel">
          <div className="panel-header">
            <h2>Personal information</h2>
            <UserRound size={18} />
          </div>
          <form className="panel-body" onSubmit={save}>
            <label>
              Full name
              <input
                required
                maxLength={120}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </label>
            <label>
              Email address
              <input readOnly value={user!.email} />
            </label>
            {user!.role === "TECHNICIAN" && (
              <>
                <div className="detail-properties">
                  <div>
                    <span>Availability</span>
                    <strong>
                      {user!.available ? "Available" : "Unavailable"}
                    </strong>
                  </div>
                  <div>
                    <span>Maximum workload</span>
                    <strong>{user!.maxWorkload}</strong>
                  </div>
                  <div>
                    <span>Experience</span>
                    <strong>{user!.experienceYears} years</strong>
                  </div>
                </div>
                <h3>Technical skills</h3>
                <div className="skill-list">
                  {user!.skills.length ? (
                    user!.skills.map((s) => <span key={s}>{s}</span>)
                  ) : (
                    <p className="muted">
                      No skills assigned. Contact your administrator.
                    </p>
                  )}
                </div>
              </>
            )}
            {error && (
              <p role="alert" className="inline-error">
                {error}
              </p>
            )}
            <div className="form-actions">
              <button className="btn primary" disabled={busy}>
                {busy ? "Saving…" : "Save profile"}
              </button>
            </div>
          </form>
        </section>
      </div>
    </>
  );
}
interface Email {
  id: number;
  recipient: string;
  subject: string;
  body: string;
  deliveryStatus: string;
  sentAt: string | null;
  createdAt: string;
}
export function NotificationSettings() {
  const settings = useApi<Settings>("/settings");
  const [page, setPage] = useState(0);
  const outbox = useApi<Page<Email>>(`/email-outbox?page=${page}`);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  async function toggle() {
    if (!settings.data) return;
    setBusy(true);
    try {
      await api.put("/settings", {
        ...settings.data,
        emailEnabled: !settings.data.emailEnabled,
      });
      settings.reload();
      toast("Notification preferences saved");
    } catch (e) {
      toast(errorMessage(e), true);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeading
        eyebrow="DELIVERY CHANNELS"
        title="Notification settings"
        description="Keep people informed through the right channels."
      />
      <section className="panel channel-panel">
        <div className="channel-row">
          <span className="stat-icon blue">
            <Bell size={20} />
          </span>
          <div>
            <h3>In-app notifications</h3>
            <p>Persistent updates for incident activity and SLA events.</p>
          </div>
          <Badge value="ENABLED" />
        </div>
        <div className="channel-row">
          <span className="stat-icon purple">
            <Mail size={20} />
          </span>
          <div>
            <h3>Email notifications</h3>
            <p>
              Email for technician assignments, SLA milestones and reporter status changes.
            </p>
          </div>
          <button
            className={`toggle ${settings.data?.emailEnabled ? "on" : ""}`}
            aria-label="Enable email notifications"
            role="switch"
            aria-checked={settings.data?.emailEnabled || false}
            disabled={busy || settings.loading}
            onClick={toggle}
          >
            <span />
          </button>
        </div>
        {settings.error && <ErrorState message={settings.error} />}
      </section>
      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Email delivery outbox</h2>
            <p>SMTP delivery status and message history</p>
          </div>
          <span className="count-pill">{outbox.data?.totalElements || 0}</span>
        </div>
        {outbox.loading ? (
          <Loading />
        ) : outbox.error ? (
          <ErrorState message={outbox.error} />
        ) : outbox.data?.content.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Recipient</th>
                  <th>Subject / message</th>
                  <th>Delivery</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {outbox.data?.content.map((e) => (
                  <tr key={e.id}>
                    <td>{e.recipient}</td>
                    <td className="email-body">
                      <strong>{e.subject}</strong>
                      <small>{e.body}</small>
                    </td>
                    <td>
                      <Badge value={e.deliveryStatus} />
                      {e.sentAt && <div className="muted">{date(e.sentAt)}</div>}
                    </td>
                    <td>{date(e.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title="No email messages yet"
            text="Enabled incident notifications will appear here."
          />
        )}
        <div className="pagination">
          <span>Page {page + 1}</span>
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
              disabled={!outbox.data || page + 1 >= outbox.data.totalPages}
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
