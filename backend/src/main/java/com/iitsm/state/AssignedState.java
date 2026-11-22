package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class AssignedState extends AbstractIncidentState {
  public AssignedState() {
    super(Status.ASSIGNED, Status.ACKNOWLEDGED);
  }
}
