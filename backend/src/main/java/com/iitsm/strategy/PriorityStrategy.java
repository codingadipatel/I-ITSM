package com.iitsm.strategy;

import com.iitsm.entity.*;
import com.iitsm.model.Priority;

public interface PriorityStrategy {
  String key();

  Result calculate(Incident incident, SystemSettings settings);

  record Result(Priority priority, double score, String reason) {}

  default Result result(double score, SystemSettings s, String reason) {
    return new Result(
        score >= s.getP1Threshold()
            ? Priority.P1
            : score >= s.getP2Threshold()
                ? Priority.P2
                : score >= s.getP3Threshold() ? Priority.P3 : Priority.P4,
        score,
        reason);
  }
}
