package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class ClosedState extends AbstractIncidentState {
  public ClosedState() {
    super(Status.CLOSED, Status.IN_PROGRESS);
  }
}
