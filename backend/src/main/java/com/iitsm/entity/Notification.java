package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User recipient;

  @ManyToOne(fetch = FetchType.LAZY)
  private Incident incident;
  private String eventType;

  @Column(columnDefinition = "text")
  private String message;

  private boolean read;
  private Instant createdAt = Instant.now();

  public Notification(User u, Incident i, String type, String m) {
    recipient = u;
    incident = i;
    eventType = type;
    message = m;
  }

  public void markRead() {
    read = true;
  }
}
