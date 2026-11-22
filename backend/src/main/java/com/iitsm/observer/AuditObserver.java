package com.iitsm.observer;

import com.iitsm.entity.AuditLog;
import com.iitsm.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditObserver implements IncidentEventListener {
  private final AuditLogRepository repository;

  public AuditObserver(AuditLogRepository repository) {
    this.repository = repository;
  }

  public void onEvent(IncidentEvent e) {
    repository.save(
        new AuditLog(
            e.incident(), e.actor(), e.action(), e.previousState(), e.newState(), e.description()));
  }
}
