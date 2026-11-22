package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class ClassifiedState extends AbstractIncidentState {
  public ClassifiedState() {
    super(Status.CLASSIFIED, Status.ASSIGNED);
  }
}
