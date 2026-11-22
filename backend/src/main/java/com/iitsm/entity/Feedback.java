package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(unique = true)
  private Incident incident;

  private int rating;

  @Column(columnDefinition = "text")
  private String comment;

  private Instant createdAt = Instant.now();

  public Feedback(Incident i, int r, String c) {
    if (r < 1 || r > 5) throw new IllegalArgumentException("Rating must be 1–5");
    incident = i;
    rating = r;
    comment = c;
  }
}
