package com.iitsm.dto;

import com.iitsm.model.*;
import java.time.Instant;
import java.util.*;

public final class Views {
  public record UserView(
      Long id,
      String name,
      String email,
      Role role,
      boolean active,
      boolean available,
      int maxWorkload,
      int experienceYears,
      Set<String> skills) {}

  public record Auth(String token, UserView user) {}

  public record Catalog(
      Long id, String name, boolean active, int criticality, String requiredSkill) {}

  public record IncidentView(
      Long id,
      long version,
      String title,
      String description,
      UserView reporter,
      UserView technician,
      Catalog service,
      Catalog category,
      int impact,
      int urgency,
      int affectedUsers,
      Priority priority,
      double priorityScore,
      String priorityReason,
      String assignmentReason,
      Status status,
      Instant createdAt,
      Instant updatedAt,
      Instant responseDeadline,
      Instant resolutionDeadline,
      Instant acknowledgedAt,
      Instant resolvedAt,
      Instant verifiedAt,
      Instant closedAt,
      String resolutionDetails,
      String verificationDetails,
      SlaLevel slaLevel,
      boolean responseViolated,
      boolean resolutionViolated) {}

  public record Note(
      Long id, UserView author, String content, boolean informationRequest, Instant createdAt) {}

  public record Audit(
      Long id,
      Long incidentId,
      String actor,
      String action,
      String previousState,
      String newState,
      String description,
      Instant createdAt) {}

  public record Duplicate(
      Long id,
      Long incidentId,
      Long relatedId,
      String relatedTitle,
      double score,
      String decision) {}

  public record NotificationConversation(long technicianId, String participantName, boolean canReply) {}

  public record NotificationView(
      Long id,
      Long incidentId,
      String eventType,
      String message,
      boolean read,
      Instant createdAt,
      NotificationConversation conversation) {}

  public record DetailView(
      IncidentView incident,
      List<Note> notes,
      List<Audit> history,
      List<Duplicate> duplicates,
      Map<String, Object> feedback) {}

  public record Recommendation(
      Long technicianId, String technicianName, String reason, double score) {}
}
