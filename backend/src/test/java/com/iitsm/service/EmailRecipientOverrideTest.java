package com.iitsm.service;

import com.iitsm.entity.Technician;
import com.iitsm.repository.SystemSettingsRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EmailRecipientOverrideTest {
  private final JdbcTemplate db = mock(JdbcTemplate.class);
  private final IncidentEmailService service = new IncidentEmailService(db,
      mock(SystemSettingsRepository.class),
      "technician@iitsm.local=shared-support@example.com,marcus@iitsm.local=shared-support@example.com");

  @ParameterizedTest
  @CsvSource({
      "technician@iitsm.local,shared-support@example.com,PENDING",
      "marcus@iitsm.local,shared-support@example.com,PENDING",
      "shared-support@example.com,shared-support@example.com,PENDING",
      "other@example.com,other@example.com,PENDING",
      "employee@iitsm.local,employee@iitsm.local,SKIPPED_LOCAL_ADDRESS"
  })
  void routesOnlyConfiguredAccountsAndChecksFinalDestination(String source, String target, String status) {
    var user = new Technician("Technician", source, "unused");
    ReflectionTestUtils.invokeMethod(service, "queue", user, "Assignment", "Body", null, null);
    verify(db).update(anyString(), eq(target), eq("[I-ITSM] Assignment"), eq("Body"),
        eq(status), isNull(), isNull());
    assertEquals(source, user.getEmail());
  }

  @Test
  void doesNotQueueInactiveAccount() {
    var user = new Technician("Technician", "marcus@iitsm.local", "unused");
    user.update("Technician", false);
    ReflectionTestUtils.invokeMethod(service, "queue", user, "Assignment", "Body", null, null);
    verifyNoInteractions(db);
  }
}
