package com.iitsm.service;

import com.iitsm.entity.*;
import com.iitsm.model.*;
import com.iitsm.observer.*;
import com.iitsm.repository.IncidentRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaMonitoringService {
  private final IncidentRepository incidents;
  private final IncidentEventPublisher events;
  private final IncidentEmailService email;

  public SlaMonitoringService(IncidentRepository i, IncidentEventPublisher e, IncidentEmailService email) {
    incidents = i;
    events = e;
    this.email = email;
  }

  @Transactional
  public void scan() {
    scanAt(Instant.now());
  }

  @Transactional
  public void scanAt(Instant now) {
    boolean emailEnabled = email.enabled();
    for (var i : incidents.findAll()) {
      if (i.getStatus() == Status.CLOSED) continue;
      if (emailEnabled) email.queueSlaReminders(i, now);
      SlaLevel previous = i.getSla().getWarningLevel();
      boolean response = i.getSla().isResponseViolated(),
          resolution = i.getSla().isResolutionViolated();
      var current = i.getSla().detect(now);
      if (current != previous
          || response != i.getSla().isResponseViolated()
          || resolution != i.getSla().isResolutionViolated()) {
        events.publish(
            new IncidentEvent(
                i,
                null,
                current == SlaLevel.VIOLATED ? "SLA_VIOLATED_ESCALATION" : "SLA_WARNING",
                previous.name(),
                current.name(),
                "SLA "
                    + current
                    + "; response violated="
                    + i.getSla().isResponseViolated()
                    + ", resolution violated="
                    + i.getSla().isResolutionViolated()));
      }
    }
  }
}
