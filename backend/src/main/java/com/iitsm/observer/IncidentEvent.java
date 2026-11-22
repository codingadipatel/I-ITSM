package com.iitsm.observer;

import com.iitsm.entity.*;
import com.iitsm.model.Status;

public record IncidentEvent(
    Incident incident,
    User actor,
    String action,
    String previousState,
    String newState,
    String description,
    Status statusBefore,
    Status statusAfter) {

  public IncidentEvent(Incident incident, User actor, String action, String previousState,
                       String newState, String description) {
    this(incident, actor, action, previousState, newState, description, null, null);
  }

  public boolean statusChanged() {
    return statusBefore != null && statusAfter != null && statusBefore != statusAfter;
  }
}
