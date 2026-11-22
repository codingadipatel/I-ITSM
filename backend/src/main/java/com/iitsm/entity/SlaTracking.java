package com.iitsm.entity;

import com.iitsm.model.SlaLevel;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor
public class SlaTracking {
  private Instant responseDeadline;
  private Instant resolutionDeadline;
  private Instant acknowledgedAt;
  private Instant resolvedAt;
  private boolean responseViolated;
  private boolean resolutionViolated;

  @Enumerated(EnumType.STRING)
  private SlaLevel warningLevel = SlaLevel.SAFE;

  public void plan(Instant created, SlaPolicy policy) {
    responseDeadline = created.plusSeconds(policy.getResponseMinutes() * 60L);
    resolutionDeadline = created.plusSeconds(policy.getResolutionMinutes() * 60L);
  }

  public void acknowledge(Instant now) {
    if (acknowledgedAt == null) acknowledgedAt = now;
    detect(now);
  }

  public void resolve(Instant now) {
    resolvedAt = now;
    detect(now);
  }

  public void reopen() {
    resolvedAt = null;
  }

  public SlaLevel detect(Instant now) {
    responseViolated |= (acknowledgedAt == null ? now : acknowledgedAt).isAfter(responseDeadline);
    resolutionViolated |= (resolvedAt == null ? now : resolvedAt).isAfter(resolutionDeadline);
    if (responseViolated || resolutionViolated) return warningLevel = SlaLevel.VIOLATED;
    if (resolvedAt != null) return warningLevel = SlaLevel.SAFE;
    Instant deadline =
        acknowledgedAt == null && responseDeadline.isBefore(resolutionDeadline)
            ? responseDeadline
            : resolutionDeadline;
    long remaining = Duration.between(now, deadline).toSeconds();
    warningLevel =
        remaining <= 120
            ? SlaLevel.AT_RISK
            : remaining <= 600 ? SlaLevel.APPROACHING : SlaLevel.SAFE;
    return warningLevel;
  }
}
