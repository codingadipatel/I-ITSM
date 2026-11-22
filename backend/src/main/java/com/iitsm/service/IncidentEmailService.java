package com.iitsm.service;

import com.iitsm.entity.Incident;
import com.iitsm.entity.User;
import com.iitsm.model.Status;
import com.iitsm.observer.IncidentEvent;
import com.iitsm.observer.IncidentEventListener;
import com.iitsm.repository.SystemSettingsRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentEmailService implements IncidentEventListener {
  private final JdbcTemplate db;
  private final SystemSettingsRepository settings;

  private final Map<String, String> recipientOverrides = new HashMap<>();

  public IncidentEmailService(JdbcTemplate db, SystemSettingsRepository settings,
      @Value("${app.email-recipient-overrides:}") String overrides) {
    this.db = db;
    this.settings = settings;
    for (String entry : overrides.split(",")) {
      if (entry.isBlank()) continue;
      String[] pair = entry.split("=", -1);
      if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank())
        throw new IllegalArgumentException("Email recipient overrides must use source=destination");
      recipientOverrides.put(pair[0].trim().toLowerCase(Locale.ROOT), pair[1].trim());
    }
  }

  public boolean enabled() {
    return settings.findById(1L).map(s -> s.isEmailEnabled()).orElse(true);
  }

  @Override
  public void onEvent(IncidentEvent event) {
    var incident = event.incident();
    boolean assignment = "ASSIGNED".equals(event.action()) && incident != null;
    if ((!assignment && !event.statusChanged()) || !enabled()) return;
    String reference = "INC-" + incident.getId();
    if (assignment) {
      var sla = incident.getSla();
      String response = sla.getAcknowledgedAt() == null ? "Response due: " + sla.getResponseDeadline()
          : "Response completed at: " + sla.getAcknowledgedAt();
      queue(incident.getTechnician(), reference + " assigned to you",
          "You have been assigned " + reference + ": " + incident.getTitle()
              + "\nPriority: " + incident.getPriority() + "\n" + response
              + "\nResolution due: " + sla.getResolutionDeadline()
              + "\nTimes shown in UTC. Open I-ITSM to view the incident.",
          null, null);
    }
    if (event.statusChanged()) {
      var status = event.statusAfter();
      String update = label(event.statusBefore()) + " → " + label(status);
      String body = reference + ": " + incident.getTitle() + "\nStatus: " + update;
      if (status == Status.RESOLVED) body += "\n\nResolution: " + incident.getResolutionDetails();
      queue(incident.getReporter(), reference + " " + label(status),
          body + "\n\nOpen I-ITSM to view the incident.", null, null);
    }
  }

  public void queueSlaReminders(Incident incident, Instant now) {
    var technician = incident.getTechnician();
    if (technician == null || !technician.isActive()) return;
    var sla = incident.getSla();
    if (sla.getAcknowledgedAt() == null)
      queueMilestones(incident, technician, "Response", sla.getResponseDeadline(), now);
    if (sla.getResolvedAt() == null)
      queueMilestones(incident, technician, "Resolution", sla.getResolutionDeadline(), now);
  }

  private void queueMilestones(Incident incident, User technician, String clock, Instant deadline,
                               Instant now) {
    Instant created = incident.getCreatedAt();
    if (deadline == null || !deadline.isAfter(created) || !now.isBefore(deadline)) return;
    Duration duration = Duration.between(created, deadline);
    for (int percent : new int[]{50, 90}) {
      if (now.isBefore(created.plus(duration.multipliedBy(percent).dividedBy(100)))) continue;
      String reference = "INC-" + incident.getId();
      String key = reference + ":" + technician.getId() + ":" + clock + ":" + deadline + ":" + percent;
      queue(technician, reference + " " + clock + " SLA reminder (" + percent + "%)",
          reference + ": " + incident.getTitle() + "\n" + clock + " SLA has reached " + percent
              + "% of its allowed time.\nDeadline (UTC): " + deadline
              + "\nOpen I-ITSM to review the incident.", key, deadline);
    }
  }

  private void queue(User recipient, String subject, String body, String key, Instant expiresAt) {
    if (!recipient.isActive()) return;
    String address = recipientOverrides.getOrDefault(
        recipient.getEmail().toLowerCase(Locale.ROOT), recipient.getEmail());
    String status = address.toLowerCase(Locale.ROOT).endsWith(".local")
        ? "SKIPPED_LOCAL_ADDRESS" : "PENDING";
    db.update("""
        INSERT INTO email_outbox(recipient, subject, body, delivery_status, created_at,
                                 deduplication_key, expires_at)
        VALUES (?, ?, ?, ?, now(), ?, ?)
        ON CONFLICT (deduplication_key) DO NOTHING
        """, address, "[I-ITSM] " + subject, body, status, key,
        expiresAt == null ? null : Timestamp.from(expiresAt));
  }

  private static String label(Status status) {
    return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }
}
