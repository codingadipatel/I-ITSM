package com.iitsm.scheduler;

import com.iitsm.service.SmtpOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {
  private final SmtpOutboxService outbox;
  private final boolean configured;

  public EmailScheduler(SmtpOutboxService outbox, @Value("${spring.mail.host}") String host,
                        @Value("${app.smtp-from}") String from) {
    this.outbox = outbox;
    this.configured = !host.isBlank() && !from.isBlank();
  }

  @Scheduled(fixedDelayString = "${SMTP_DISPATCH_INTERVAL_MS:3000}", initialDelay = 3000)
  public void sendPendingEmail() {
    if (!configured) return;
    for (int count = 0; count < 10; count++) {
      if (!outbox.deliverNext()) break;
    }
  }
}
