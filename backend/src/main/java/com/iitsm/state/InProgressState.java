package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class InProgressState extends AbstractIncidentState {
  public InProgressState() {
    super(Status.IN_PROGRESS, Status.RESOLVED);
  }
}
