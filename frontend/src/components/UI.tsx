import { useEffect, useRef, type ReactNode } from "react";
import { AlertCircle, Inbox, LoaderCircle, X } from "lucide-react";
import { label } from "../utils/format";
export function Badge({
  value,
  type = "status",
}: {
  value: string;
  type?: "status" | "priority" | "sla";
}) {
  return (
    <span className={`badge ${type}-${value.toLowerCase()}`}>
      <i />
      {type === "priority"
        ? `${value} ${({ P1: "Critical", P2: "High", P3: "Medium", P4: "Low" } as Record<string, string>)[value]}`
        : label(value)}
    </span>
  );
}
export function Loading() {
  return (
    <div className="state-box" role="status">
      <LoaderCircle className="spin" size={24} />
      <span>Loading your workspace…</span>
    </div>
  );
}
export function ErrorState({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div className="state-box error" role="alert">
      <AlertCircle size={26} />
      <p>{message}</p>
      {retry && (
        <button className="btn secondary" onClick={retry}>
          Try again
        </button>
      )}
    </div>
  );
}
export function Empty({
  title = "Nothing here yet",
  text = "New activity will appear here.",
  action,
}: {
  title?: string;
  text?: string;
  action?: ReactNode;
}) {
  return (
    <div className="state-box">
      <div className="empty-icon">
        <Inbox size={27} />
      </div>
      <h3>{title}</h3>
      <p>{text}</p>
      {action}
    </div>
  );
}
export function PageHeading({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current!;
    d.showModal();
    return () => d.close();
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="modal-header">
        <h2>{title}</h2>
        <button
          className="icon-button"
          aria-label="Close dialog"
          onClick={onClose}
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function Confirm({
  title,
  text,
  onConfirm,
  onClose,
  busy = false,
}: {
  title: string;
  text: string;
  onConfirm: () => void;
  onClose: () => void;
  busy?: boolean;
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="muted">{text}</p>
      <div className="form-actions">
        <button className="btn secondary" onClick={onClose}>
          Cancel
        </button>
        <button className="btn primary" disabled={busy} onClick={onConfirm}>
          {busy ? "Saving…" : "Confirm"}
        </button>
      </div>
    </Modal>
  );
}
