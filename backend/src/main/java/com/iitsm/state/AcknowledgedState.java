package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class AcknowledgedState extends AbstractIncidentState {
  public AcknowledgedState() {
    super(Status.ACKNOWLEDGED, Status.IN_PROGRESS);
  }
}
