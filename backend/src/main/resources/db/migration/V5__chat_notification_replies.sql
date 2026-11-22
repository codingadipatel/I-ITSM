ALTER TABLE incident_chat_messages
    ADD COLUMN source_notification_id BIGINT REFERENCES notifications(id) ON DELETE SET NULL;
