package com.iitsm.observer;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EventPublisherTest {
  @Test
  void publisherNotifiesEveryRegisteredObserver() {
    var first = mock(IncidentEventListener.class);
    var second = mock(IncidentEventListener.class);
    var event = new IncidentEvent(null, null, "TEST", null, null, "Event");
    new IncidentEventPublisher(List.of(first, second)).publish(event);
    verify(first).onEvent(event);
    verify(second).onEvent(event);
  }
}
