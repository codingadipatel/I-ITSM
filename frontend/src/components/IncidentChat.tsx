import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, errorMessage, get } from "../services/api";
import { date } from "../utils/format";

interface Message {
  id: number;
  senderId: number;
  sender: string;
  body: string;
  sentAt: string;
  notificationReply: boolean;
}

interface Props {
  incidentId: number;
  technicianId: number;
  participantName: string;
  userId: number;
  canReply: boolean;
  notificationId?: number;
  onSent?: () => void;
}

export function IncidentChat({
  incidentId, technicianId, participantName, userId, canReply, notificationId, onSent,
}: Props) {
  const path = `/incidents/${incidentId}/chat`;
  const [messages, setMessages] = useState<Message[] | null>(null);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState("");
  const [sendError, setSendError] = useState("");
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  const history = useRef<HTMLDivElement>(null);
  const followLatest = useRef(true);

  useEffect(() => {
    let active = true;
    let timer: ReturnType<typeof setTimeout>;
    async function refresh() {
      try {
        const rows = await get<Message[]>(`${path}?technicianId=${technicianId}`);
        if (active) {
          setMessages((previous) => previous?.length === rows.length ? previous : rows);
          setError("");
        }
      } catch (e) {
        if (active) {
          setMessages(null);
          setError(errorMessage(e));
        }
      } finally {
        if (active) timer = setTimeout(refresh, 3000);
      }
    }
    refresh();
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [path, technicianId, revision]);

  useEffect(() => {
    if (history.current && followLatest.current) history.current.scrollTop = history.current.scrollHeight;
  }, [messages]);

  async function send(e: FormEvent) {
    e.preventDefault();
    if (!canReply || busy || !messages || !draft.trim()) return;
    setBusy(true);
    setSendError("");
    try {
      const target = notificationId ? `/notifications/${notificationId}/reply` : path;
      await api.post(target, { technicianId, body: draft.trim() });
      setDraft("");
      followLatest.current = true;
      setRevision(r => r + 1);
      onSent?.();
    } catch (e) {
      setSendError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="panel-body incident-chat">
      <h3>Chat with {participantName}</h3>
      <div
        ref={history}
        className="chat-history"
        role="log"
        aria-label="Incident chat"
        onScroll={(e) => {
          const view = e.currentTarget;
          followLatest.current = view.scrollHeight - view.scrollTop - view.clientHeight < 48;
        }}
      >
        {messages?.map(m => (
          <article className="note" key={m.id}>
            <div>
              <strong>{m.senderId === userId ? "You" : m.sender}</strong>{" "}
              <small>{date(m.sentAt)}</small>
              {m.notificationReply && <small className="chat-message-source">Notification reply</small>}
              <p className="pre-wrap">{m.body}</p>
            </div>
          </article>
        ))}
        {!error && (messages === null ? <p>Loading messages…</p> : messages.length === 0 && <p>No messages yet.</p>)}
      </div>
      {error && <p role="alert">{error}</p>}
      {canReply ? (
        <form onSubmit={send}>
          <label>
            Message
            <textarea required maxLength={4000} value={draft} disabled={busy}
              onChange={e => setDraft(e.target.value)} rows={2} />
          </label>
          {sendError && <p role="alert">{sendError}</p>}
          <button className="btn primary" disabled={busy || !draft.trim() || !messages}>Send message</button>
        </form>
      ) : <p>This incident is closed.</p>}
    </div>
  );
}
