package com.iitsm.scheduler;

import com.iitsm.service.SlaMonitoringService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaScheduler {
  private final SlaMonitoringService monitor;

  public SlaScheduler(SlaMonitoringService m) {
    monitor = m;
  }

  @Scheduled(fixedDelayString = "${app.sla-interval-ms}", initialDelay = 10000)
  public void monitor() {
    monitor.scan();
  }
}
