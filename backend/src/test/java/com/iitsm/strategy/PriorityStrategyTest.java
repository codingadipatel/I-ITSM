package com.iitsm.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.entity.*;
import com.iitsm.model.*;
import org.junit.jupiter.api.*;

class PriorityStrategyTest {
  private Incident incident(int impact, int urgency, int users) {
    return new Incident(
        new Employee("A", "a@b.test", "hash"),
        "Payment outage",
        "Payments are failing",
        new ServiceItem("Payments", 3, "Applications"),
        new IncidentCategory("Failure", "Applications"),
        impact,
        urgency,
        users);
  }

  @Test
  void criticalBusinessOutage() {
    var result =
        new WeightedPriorityStrategy().calculate(incident(3, 3, 100), new SystemSettings());
    assertEquals(Priority.P1, result.priority());
    assertEquals(40.5, result.score());
    assertTrue(result.reason().contains("1.5"));
  }

  @Test
  void userBandBoundaries() {
    var s = new WeightedPriorityStrategy();
    assertEquals(9, s.calculate(incident(1, 3, 9), new SystemSettings()).score());
    assertEquals(10.8, s.calculate(incident(1, 3, 10), new SystemSettings()).score(), .001);
    assertEquals(13.5, s.calculate(incident(1, 3, 100), new SystemSettings()).score());
  }

  @Test
  void customThresholdAtEquality() {
    var c = new SystemSettings();
    c.configure("SKILL", "WEIGHTED", 27, 18, 9, true);
    assertEquals(
        Priority.P1, new WeightedPriorityStrategy().calculate(incident(3, 3, 1), c).priority());
    assertEquals(
        Priority.P3, new WeightedPriorityStrategy().calculate(incident(1, 3, 1), c).priority());
  }

  @Test
  void lowPriority() {
    assertEquals(
        Priority.P4,
        new WeightedPriorityStrategy()
            .calculate(incident(1, 1, 1), new SystemSettings())
            .priority());
  }

  @Test
  void algorithmsInterchangeable() {
    PriorityStrategy strategy = new RuleBasedPriorityStrategy();
    assertEquals(30, strategy.calculate(incident(3, 3, 100), new SystemSettings()).score());
  }

  @Test
  void invalidThresholdOrder() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SystemSettings().configure("LOAD", "RULE", 5, 10, 3, true));
  }

  @Test
  void invalidImpactRejected() {
    assertThrows(IllegalArgumentException.class, () -> incident(0, 2, 1));
  }
}
