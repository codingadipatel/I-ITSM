ALTER TABLE email_outbox
  ADD COLUMN deduplication_key TEXT UNIQUE,
  ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN expires_at TIMESTAMPTZ,
  ADD COLUMN sent_at TIMESTAMPTZ;

CREATE INDEX email_outbox_pending ON email_outbox(next_attempt_at, id)
  WHERE delivery_status IN ('PENDING', 'RETRYING');
