package com.iitsm.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmtpOutboxService {
  private static final Logger log = LoggerFactory.getLogger(SmtpOutboxService.class);
  private static final int MAX_ATTEMPTS = 3;
  private final JdbcTemplate db;
  private final JavaMailSender sender;
  private final String from;

  private record Pending(long id, String recipient, String subject, String body, int attempts,
                         boolean expired) {}

  public SmtpOutboxService(JdbcTemplate db, JavaMailSender sender, @Value("${app.smtp-from}") String from) {
    this.db = db;
    this.sender = sender;
    this.from = from;
  }

  @Transactional
  public boolean deliverNext() {
    var rows = db.query("""
        SELECT id, recipient, subject, body, attempts, coalesce(expires_at <= now(), false) AS expired
        FROM email_outbox
        WHERE delivery_status IN ('PENDING', 'RETRYING') AND next_attempt_at <= now()
        ORDER BY next_attempt_at, id
        LIMIT 1 FOR UPDATE SKIP LOCKED
        """, (r, n) -> new Pending(r.getLong("id"), r.getString("recipient"),
            r.getString("subject"), r.getString("body"), r.getInt("attempts"),
            r.getBoolean("expired")));
    if (rows.isEmpty()) return false;
    var email = rows.getFirst();
    if (email.expired()) {
      db.update("UPDATE email_outbox SET delivery_status = 'EXPIRED' WHERE id = ?", email.id());
      return true;
    }
    try {
      var message = sender.createMimeMessage();
      var helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(new InternetAddress(from, true));
      var recipient = new InternetAddress(email.recipient(), true);
      recipient.validate();
      if (recipient.isGroup()) throw new MessagingException("Recipient must be a single mailbox");
      helper.setTo(recipient);
      helper.setSubject(email.subject());
      helper.setText(email.body());
      sender.send(message);
    } catch (MailException | MessagingException failure) {
      int attempts = email.attempts() + 1;
      String status = attempts >= MAX_ATTEMPTS ? "FAILED" : "RETRYING";
      db.update("""
          UPDATE email_outbox SET delivery_status = ?, attempts = ?,
              next_attempt_at = now() + interval '1 minute' * ?
          WHERE id = ?
          """, status, attempts, attempts, email.id());
      log.warn("SMTP delivery for outbox row {}: {} ({})", email.id(), status,
          failure.getClass().getSimpleName());
      return true;
    }
    db.update("""
        UPDATE email_outbox SET delivery_status = 'SENT', attempts = attempts + 1, sent_at = now()
        WHERE id = ?
        """, email.id());
    return true;
  }
}
