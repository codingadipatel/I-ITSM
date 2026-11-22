package com.iitsm.entity;

import com.iitsm.model.Priority;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlaPolicy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(unique = true, nullable = false)
  private Priority priority;

  private int responseMinutes;
  private int resolutionMinutes;

  public SlaPolicy(Priority p, int r, int s) {
    priority = p;
    configure(r, s);
  }

  public void configure(int r, int s) {
    if (r < 1 || s < r || s > 525600)
      throw new IllegalArgumentException(
          "Resolution target must be >= response target (1–525600 minutes)");
    responseMinutes = r;
    resolutionMinutes = s;
  }
}
