package com.iitsm.mapper;

import com.iitsm.dto.Views.*;
import com.iitsm.entity.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ViewMapper {
  public UserView user(User u) {
    if (u == null) return null;
    return u instanceof Technician t
        ? new UserView(
            u.getId(),
            u.getName(),
            u.getEmail(),
            u.getRole(),
            u.isActive(),
            t.isAvailable(),
            t.getMaxWorkload(),
            t.getExperienceYears(),
            t.getSkills().stream().map(Skill::getName).collect(java.util.stream.Collectors.toSet()))
        : new UserView(
            u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(), false, 0, 0, Set.of());
  }

  public Catalog service(ServiceItem s) {
    return new Catalog(
        s.getId(), s.getName(), s.isActive(), s.getCriticality(), s.getRequiredSkill());
  }

  public Catalog category(IncidentCategory c) {
    return new Catalog(c.getId(), c.getName(), c.isActive(), 1, c.getRequiredSkill());
  }

  public IncidentView incident(Incident i) {
    var s = i.getSla();
    return new IncidentView(
        i.getId(),
        i.getVersion(),
        i.getTitle(),
        i.getDescription(),
        user(i.getReporter()),
        user(i.getTechnician()),
        service(i.getService()),
        category(i.getCategory()),
        i.getImpact(),
        i.getUrgency(),
        i.getAffectedUsers(),
        i.getPriority(),
        i.getPriorityScore(),
        i.getPriorityReason(),
        i.getAssignmentReason(),
        i.getStatus(),
        i.getCreatedAt(),
        i.getUpdatedAt(),
        s.getResponseDeadline(),
        s.getResolutionDeadline(),
        s.getAcknowledgedAt(),
        s.getResolvedAt(),
        i.getVerifiedAt(),
        i.getClosedAt(),
        i.getResolutionDetails(),
        i.getVerificationDetails(),
        s.getWarningLevel(),
        s.isResponseViolated(),
        s.isResolutionViolated());
  }

  public Audit audit(AuditLog a) {
    return new Audit(
        a.getId(),
        a.getIncident() == null ? null : a.getIncident().getId(),
        a.getActor() == null ? "System" : a.getActor().getName(),
        a.getAction(),
        a.getPreviousState(),
        a.getNewState(),
        a.getDescription(),
        a.getCreatedAt());
  }

  public Note note(IncidentNote n) {
    return new Note(
        n.getId(), user(n.getAuthor()), n.getContent(), n.isInformationRequest(), n.getCreatedAt());
  }

  public Duplicate duplicate(DuplicateSuggestion d, Long current) {
    var r = d.getIncident().getId().equals(current) ? d.getRelated() : d.getIncident();
    return new Duplicate(
        d.getId(), d.getIncident().getId(), r.getId(), r.getTitle(), d.getScore(), d.getDecision());
  }

  public NotificationView notification(Notification n, NotificationConversation conversation) {
    return new NotificationView(
        n.getId(),
        n.getIncident() == null ? null : n.getIncident().getId(),
        n.getEventType(),
        n.getMessage(),
        n.isRead(),
        n.getCreatedAt(),
        conversation);
  }
}
