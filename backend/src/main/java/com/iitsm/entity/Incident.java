package com.iitsm.entity;

import com.iitsm.model.*;
import com.iitsm.state.IncidentState;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "incidents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @ManyToOne(optional = false)
  private User reporter;

  @ManyToOne private Technician technician;

  @ManyToOne(optional = false)
  private ServiceItem service;

  @ManyToOne(optional = false)
  private IncidentCategory category;

  private int impact;
  private int urgency;
  private int affectedUsers;
  private double priorityScore;

  @Enumerated(EnumType.STRING)
  private Priority priority;

  private String priorityReason;
  private String assignmentReason;

  @Enumerated(EnumType.STRING)
  private Status status = Status.NEW;

  @ManyToOne private SlaPolicy slaPolicy;
  @Embedded private SlaTracking sla = new SlaTracking();
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  private Instant verifiedAt;
  private Instant closedAt;

  @Column(columnDefinition = "text")
  private String resolutionDetails;

  @Column(columnDefinition = "text")
  private String verificationDetails;

  public Incident(
      User reporter,
      String title,
      String description,
      ServiceItem service,
      IncidentCategory category,
      int impact,
      int urgency,
      int users) {
    this.reporter = reporter;
    revise(title, description, service, category, impact, urgency, users);
  }

  public void revise(
      String title,
      String description,
      ServiceItem service,
      IncidentCategory category,
      int impact,
      int urgency,
      int users) {
    if (impact < 1 || impact > 3 || urgency < 1 || urgency > 3 || users < 1 || users > 1000000)
      throw new IllegalArgumentException("Impact/urgency must be 1–3 and affected users 1–1000000");
    this.title = title;
    this.description = description;
    this.service = service;
    this.category = category;
    this.impact = impact;
    this.urgency = urgency;
    affectedUsers = users;
    touch();
  }

  public void prioritize(Priority p, double score, String reason, SlaPolicy policy) {
    priority = p;
    priorityScore = score;
    priorityReason = reason;
    slaPolicy = policy;
    sla.plan(createdAt, policy);
    touch();
  }

  public void assign(Technician tech, String reason) {
    technician = tech;
    assignmentReason = reason;
    touch();
  }

  public void transition(IncidentState state, Status target) {
    state.transition(this, target);
    status = target;
    touch();
  }

  public void acknowledge(Instant now) {
    sla.acknowledge(now);
  }

  public void resolve(String detail, Instant now) {
    if (detail == null || detail.trim().length() < 10)
      throw new IllegalArgumentException("Provide at least 10 characters of resolution details");
    resolutionDetails = detail;
    sla.resolve(now);
  }

  public void verify(String detail, Instant now) {
    verificationDetails = detail;
    verifiedAt = now;
  }

  public void close(Instant now) {
    closedAt = now;
  }

  public void reopen() {
    sla.reopen();
    verifiedAt = null;
    closedAt = null;
    verificationDetails = null;
  }

  public void touch() {
    updatedAt = Instant.now();
  }
}
