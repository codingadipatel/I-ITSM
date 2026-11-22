package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class ResolvedState extends AbstractIncidentState {
  public ResolvedState() {
    super(Status.RESOLVED, Status.VERIFIED, Status.IN_PROGRESS);
  }
}
