package com.iitsm.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class DuplicateDetectionTest {
  @Test
  void equivalentTextIgnoresCaseAndPunctuation() {
    assertEquals(
        1,
        DuplicateDetectionService.similarity(
            "Database connection failure!", "DATABASE connection failure."));
  }

  @Test
  void disjointDescriptions() {
    assertEquals(
        0,
        DuplicateDetectionService.similarity(
            "Laptop display flickering", "Database connection failure"));
  }

  @Test
  void partialOverlapJaccard() {
    assertEquals(
        .5,
        DuplicateDetectionService.similarity(
            "database connection timeout", "database connection failure"));
  }

  @Test
  void emptyTextDoesNotMatch() {
    assertEquals(0, DuplicateDetectionService.similarity("the and for", ""));
  }
}
