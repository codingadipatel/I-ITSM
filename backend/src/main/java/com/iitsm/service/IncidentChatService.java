package com.iitsm.service;

import com.iitsm.dto.Views.NotificationConversation;
import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.model.Status;
import com.iitsm.observer.InAppNotificationChannel;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.security.AccessService;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentChatService {
  public record Message(
      long id, long senderId, String sender, String body, Instant sentAt, boolean notificationReply) {}
  private final IncidentRepository incidents;
  private final AccessService access;
  private final JdbcTemplate db;

  private final InAppNotificationChannel notifications;

  public IncidentChatService(IncidentRepository incidents, AccessService access, JdbcTemplate db,
                             InAppNotificationChannel notifications) {
    this.incidents = incidents;
    this.access = access;
    this.db = db;
    this.notifications = notifications;
  }

  private User partner(Incident incident, User actor) {
    if (incident == null || incident.getTechnician() == null) return null;
    if (incident.getReporter().getId().equals(actor.getId())) return incident.getTechnician();
    if (incident.getTechnician().getId().equals(actor.getId())) return incident.getReporter();
    return null;
  }

  public NotificationConversation conversation(Incident incident, User actor) {
    var other = partner(incident, actor);
    if (other == null) return null;
    return new NotificationConversation(incident.getTechnician().getId(), other.getName(),
        incident.getStatus() != Status.CLOSED);
  }

  private User requireParticipant(Incident incident, User actor) {
    var other = partner(incident, actor);
    if (other == null) {
      throw new UnauthorizedOperationException("Chat is available to the reporter and current technician");
    }
    return other;
  }

  @Transactional(readOnly = true)
  public List<Message> messages(long id, long expectedTechnicianId) {
    var incident = incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException("Incident not found"));
    requireParticipant(incident, access.current());
    if (incident.getTechnician().getId() != expectedTechnicianId)
      throw new InvalidIncidentStateException("Technician changed. Refresh the incident.");
    return db.query("""
        SELECT m.id, m.sender_id, u.name, m.body, m.sent_at,
               m.source_notification_id IS NOT NULL AS notification_reply
        FROM incident_chat_messages m JOIN users u ON u.id = m.sender_id
        WHERE m.incident_id = ? AND m.technician_id = ? ORDER BY m.id
        """, (r, row) -> new Message(r.getLong("id"), r.getLong("sender_id"), r.getString("name"),
          r.getString("body"), r.getTimestamp("sent_at").toInstant(), r.getBoolean("notification_reply")),
        id, expectedTechnicianId);
  }

  @Transactional
  public void send(long id, long expectedTechnicianId, String body, Long sourceNotificationId) {
    var incident = incidents.findLockedById(id).orElseThrow(() -> new IncidentNotFoundException("Incident not found"));
    var actor = access.current();
    var other = requireParticipant(incident, actor);
    if (incident.getTechnician().getId() != expectedTechnicianId)
      throw new InvalidIncidentStateException("Technician changed. Refresh the incident before sending.");
    if (incident.getStatus() == Status.CLOSED)
      throw new InvalidIncidentStateException("Reopen the incident before sending a message");
    db.update("""
        INSERT INTO incident_chat_messages(incident_id, technician_id, sender_id, body, source_notification_id)
        VALUES (?, ?, ?, ?, ?)
        """, id, expectedTechnicianId, actor.getId(), body.strip(), sourceNotificationId);
    if (other.isActive() && !other.getId().equals(actor.getId())) {
      notifications.deliver(other, incident, "CHAT_MESSAGE",
          actor.getName() + " sent a message about INC-" + incident.getId() + " · " + incident.getTitle());
    }
  }
}
