package com.iitsm.state;

import com.iitsm.model.Status;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class StateRegistry {
  private final Map<Status, IncidentState> states = new EnumMap<>(Status.class);

  public StateRegistry(List<IncidentState> all) {
    all.forEach(s -> states.put(s.status(), s));
  }

  public IncidentState get(Status s) {
    return states.get(s);
  }
}
