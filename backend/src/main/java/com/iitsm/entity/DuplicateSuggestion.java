package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "duplicate_suggestions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DuplicateSuggestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne private Incident incident;
  @ManyToOne private Incident related;
  private double score;
  private String decision = "PENDING";
  @ManyToOne private User reviewedBy;
  private Instant reviewedAt;

  public DuplicateSuggestion(Incident i, Incident r, double s) {
    incident = i;
    related = r;
    score = s;
  }

  public void review(User user, String decision) {
    if (!java.util.Set.of("CONFIRMED", "REJECTED").contains(decision))
      throw new IllegalArgumentException("Decision must be CONFIRMED or REJECTED");
    this.decision = decision;
    reviewedBy = user;
    reviewedAt = Instant.now();
  }
}
