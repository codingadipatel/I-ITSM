package com.iitsm.state;

import com.iitsm.entity.Incident;
import com.iitsm.exception.InvalidIncidentStateException;
import com.iitsm.model.Status;
import java.util.Set;

public abstract class AbstractIncidentState implements IncidentState {
  private final Status status;
  private final Set<Status> targets;

  protected AbstractIncidentState(Status s, Status... allowed) {
    status = s;
    targets = Set.of(allowed);
  }

  public Status status() {
    return status;
  }

  public void transition(Incident incident, Status target) {
    if (incident.getStatus() != status || !targets.contains(target))
      throw new InvalidIncidentStateException(
          "Cannot transition " + incident.getStatus() + " → " + target);
    if (target == Status.RESOLVED
        && (incident.getResolutionDetails() == null
            || incident.getResolutionDetails().trim().length() < 10))
      throw new InvalidIncidentStateException("Resolution details are required");
    if (target == Status.ACKNOWLEDGED && incident.getTechnician() == null)
      throw new InvalidIncidentStateException("Assign a technician before acknowledgement");
  }
}
