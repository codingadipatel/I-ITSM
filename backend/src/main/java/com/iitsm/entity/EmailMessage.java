package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "email_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String recipient;
  private String subject;

  @Column(columnDefinition = "text")
  private String body;

  private String deliveryStatus;
  private Instant createdAt;
  private Instant sentAt;

}
