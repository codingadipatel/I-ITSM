package com.iitsm.state;

import com.iitsm.model.Status;
import org.springframework.stereotype.Component;

@Component
public class NewState extends AbstractIncidentState {
  public NewState() {
    super(Status.NEW, Status.CLASSIFIED);
  }
}
