package com.iitsm.strategy;

import com.iitsm.entity.*;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedPriorityStrategy implements PriorityStrategy {
  public String key() {
    return "RULE";
  }

  public Result calculate(Incident i, SystemSettings s) {
    double score =
        i.getImpact() * 3
            + i.getUrgency() * 3
            + i.getService().getCriticality() * 2
            + (i.getAffectedUsers() >= 100 ? 6 : i.getAffectedUsers() >= 10 ? 3 : 0);
    return result(
        score,
        s,
        "Rule score = impact × 3 + urgency × 3 + criticality × 2 + user band (0/3/6) = " + score);
  }
}
