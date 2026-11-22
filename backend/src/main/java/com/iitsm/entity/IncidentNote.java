package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "incident_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentNote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Incident incident;

  @ManyToOne(optional = false)
  private User author;

  @Column(columnDefinition = "text")
  private String content;

  private boolean informationRequest;
  private Instant createdAt = Instant.now();

  public IncidentNote(Incident i, User u, String text, boolean request) {
    incident = i;
    author = u;
    content = text;
    informationRequest = request;
  }
}
