package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class VerifiedState extends AbstractIncidentState {
  public VerifiedState() {
    super(Status.VERIFIED, Status.CLOSED, Status.IN_PROGRESS);
  }
}
