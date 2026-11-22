package com.iitsm.strategy;

import com.iitsm.entity.*;
import org.springframework.stereotype.Component;

@Component
public class WeightedPriorityStrategy implements PriorityStrategy {
  public String key() {
    return "WEIGHTED";
  }

  public Result calculate(Incident i, SystemSettings s) {
    double userFactor = i.getAffectedUsers() >= 100 ? 1.5 : i.getAffectedUsers() >= 10 ? 1.2 : 1.0;
    double score = i.getImpact() * i.getUrgency() * i.getService().getCriticality() * userFactor;
    return result(
        score,
        s,
        "Impact "
            + i.getImpact()
            + " × urgency "
            + i.getUrgency()
            + " × service criticality "
            + i.getService().getCriticality()
            + " × affected-user factor "
            + userFactor
            + " = "
            + score);
  }
}