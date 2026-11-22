package com.iitsm.service;

import com.iitsm.dto.Requests;
import com.iitsm.entity.*;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfigurationService {
  private final SystemSettingsRepository settings;
  private final SlaPolicyRepository policies;
  private final AccessService access;
  private final IncidentEventPublisher events;

  public ConfigurationService(
      SystemSettingsRepository s,
      SlaPolicyRepository p,
      AccessService a,
      IncidentEventPublisher e) {
    settings = s;
    policies = p;
    access = a;
    events = e;
  }

  public SystemSettings settings() {
    return settings.findById(1L).orElseGet(() -> settings.save(new SystemSettings()));
  }

  public SystemSettings update(Requests.Settings r) {
    var actor = access.current();
    access.admin(actor);
    var s = settings();
    s.configure(
        r.assignmentStrategy(),
        r.priorityStrategy(),
        r.p1Threshold(),
        r.p2Threshold(),
        r.p3Threshold(),
        r.emailEnabled());
    events.publish(
        new IncidentEvent(
            null,
            actor,
            "CONFIGURATION_UPDATED",
            null,
            null,
            "Assignment, priority and notification settings updated"));
    return s;
  }

  public List<SlaPolicy> policies() {
    return policies.findAll();
  }

  public SlaPolicy policy(Long id, Requests.Policy r) {
    var actor = access.current();
    access.admin(actor);
    SlaPolicy p =
        id == null
            ? new SlaPolicy(r.priority(), r.responseMinutes(), r.resolutionMinutes())
            : policies
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SLA policy not found"));
    if (id != null && p.getPriority() != r.priority())
      throw new IllegalArgumentException("Policy priority cannot be changed");
    p.configure(r.responseMinutes(), r.resolutionMinutes());
    policies.save(p);
    events.publish(
        new IncidentEvent(
            null,
            actor,
            "SLA_CONFIGURED",
            null,
            null,
            "Policy " + p.getPriority() + " updated; applies to new or reprioritized incidents"));
    return p;
  }
}
