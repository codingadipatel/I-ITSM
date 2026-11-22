package com.iitsm.observer;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventPublisher {
  private final List<IncidentEventListener> observers;

  public IncidentEventPublisher(List<IncidentEventListener> observers) {
    this.observers = List.copyOf(observers);
  }

  public void publish(IncidentEvent e) {
    observers.forEach(o -> o.onEvent(e));
  }
}
