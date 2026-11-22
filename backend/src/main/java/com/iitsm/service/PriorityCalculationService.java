package com.iitsm.service;

import com.iitsm.entity.*;
import com.iitsm.exception.SLAConfigurationNotFoundException;
import com.iitsm.repository.SlaPolicyRepository;
import com.iitsm.strategy.PriorityStrategy;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PriorityCalculationService {
  private final Map<String, PriorityStrategy> strategies = new HashMap<>();
  private final ConfigurationService config;
  private final SlaPolicyRepository policies;

  public PriorityCalculationService(
      List<PriorityStrategy> all, ConfigurationService c, SlaPolicyRepository p) {
    all.forEach(s -> strategies.put(s.key(), s));
    config = c;
    policies = p;
  }

  public void calculate(Incident i) {
    var settings = config.settings();
    var r = strategies.get(settings.getPriorityStrategy()).calculate(i, settings);
    var policy =
        policies
            .findByPriority(r.priority())
            .orElseThrow(
                () -> new SLAConfigurationNotFoundException("Configure SLA for " + r.priority()));
    i.prioritize(r.priority(), r.score(), r.reason(), policy);
  }
}
