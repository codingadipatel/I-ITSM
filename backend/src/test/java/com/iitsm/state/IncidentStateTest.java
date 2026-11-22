package com.iitsm.state;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.model.*;
import org.junit.jupiter.api.*;

class IncidentStateTest {
  Incident incident() {
    return new Incident(
        new Employee("A", "a@b.test", "hash"),
        "Network failure",
        "The network is offline",
        new ServiceItem("Network", 3, "Network"),
        new IncidentCategory("Connectivity", "Network"),
        3,
        3,
        100);
  }

  @Test
  void cannotSkipToClosed() {
    var i = incident();
    assertThrows(
        InvalidIncidentStateException.class, () -> i.transition(new NewState(), Status.CLOSED));
    assertEquals(Status.NEW, i.getStatus());
  }

  @Test
  void fullStateChainAndReopening() {
    var i = incident();
    i.prioritize(Priority.P1, 27, "test", new SlaPolicy(Priority.P1, 5, 30));
    i.transition(new NewState(), Status.CLASSIFIED);
    i.assign(new Technician("T", "t@b", "hash"), "Matched");
    i.transition(new ClassifiedState(), Status.ASSIGNED);
    i.transition(new AssignedState(), Status.ACKNOWLEDGED);
    i.transition(new AcknowledgedState(), Status.IN_PROGRESS);
    i.resolve("Replaced failing router", java.time.Instant.now());
    i.transition(new InProgressState(), Status.RESOLVED);
    i.transition(new ResolvedState(), Status.VERIFIED);
    i.transition(new VerifiedState(), Status.CLOSED);
    i.transition(new ClosedState(), Status.IN_PROGRESS);
    assertEquals(Status.IN_PROGRESS, i.getStatus());
  }

  @Test
  void unassignedCannotAcknowledge() {
    var i = incident();
    i.transition(new NewState(), Status.CLASSIFIED);
    i.transition(new ClassifiedState(), Status.ASSIGNED);
    assertThrows(
        InvalidIncidentStateException.class,
        () -> i.transition(new AssignedState(), Status.ACKNOWLEDGED));
  }

  @Test
  void insufficientResolutionRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> incident().resolve("fixed", java.time.Instant.now()));
  }
}
