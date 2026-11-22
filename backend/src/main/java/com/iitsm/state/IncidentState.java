package com.iitsm.state;

import com.iitsm.entity.Incident;
import com.iitsm.model.Status;

public interface IncidentState {
  Status status();

  void transition(Incident incident, Status target);
}
