package com.iitsm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.entity.*;
import com.iitsm.model.*;
import java.time.*;
import org.junit.jupiter.api.*;

class SlaTrackingTest {
  Instant start = Instant.parse("2026-01-01T00:00:00Z");

  SlaTracking planned() {
    var s = new SlaTracking();
    s.plan(start, new SlaPolicy(Priority.P2, 15, 120));
    return s;
  }

  @Test
  void targetCalculationUsesAbsoluteTime() {
    var s = planned();
    assertEquals(start.plusSeconds(900), s.getResponseDeadline());
    assertEquals(start.plusSeconds(7200), s.getResolutionDeadline());
  }

  @Test
  void warningBands() {
    var s = planned();
    assertEquals(SlaLevel.SAFE, s.detect(start));
    assertEquals(SlaLevel.APPROACHING, s.detect(start.plusSeconds(301)));
    assertEquals(SlaLevel.AT_RISK, s.detect(start.plusSeconds(800)));
  }

  @Test
  void exactDeadlineNotViolationButAfterIs() {
    var s = planned();
    assertNotEquals(SlaLevel.VIOLATED, s.detect(start.plusSeconds(900)));
    assertEquals(SlaLevel.VIOLATED, s.detect(start.plusSeconds(901)));
    assertTrue(s.isResponseViolated());
  }

  @Test
  void acknowledgementStopsResponseClock() {
    var s = planned();
    s.acknowledge(start.plusSeconds(60));
    assertEquals(SlaLevel.SAFE, s.detect(start.plusSeconds(2000)));
  }

  @Test
  void resolvedInTimeRemainsSafeLater() {
    var s = planned();
    s.acknowledge(start.plusSeconds(60));
    s.resolve(start.plusSeconds(3600));
    assertEquals(SlaLevel.SAFE, s.detect(start.plusSeconds(9000)));
  }

  @Test
  void breachPersistsAcrossReplan() {
    var s = planned();
    s.detect(start.plusSeconds(901));
    s.plan(start, new SlaPolicy(Priority.P4, 240, 1440));
    assertTrue(s.isResponseViolated());
  }
}
