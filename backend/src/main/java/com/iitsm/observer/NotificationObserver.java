package com.iitsm.observer;

import com.iitsm.entity.*;
import com.iitsm.model.Role;
import com.iitsm.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class NotificationObserver implements IncidentEventListener {
  private final InAppNotificationChannel inApp;
  private final UserRepository users;

  public NotificationObserver(InAppNotificationChannel inApp, UserRepository users) {
    this.inApp = inApp;
    this.users = users;
  }

  public void onEvent(IncidentEvent e) {
    if (e.incident() == null) return;
    var i = e.incident();
    Map<Long, User> recipients = new LinkedHashMap<>();
    recipients.put(i.getReporter().getId(), i.getReporter());
    if (i.getTechnician() != null) recipients.put(i.getTechnician().getId(), i.getTechnician());
    if (e.action().startsWith("SLA_"))
      users.findAll().stream()
          .filter(u -> u.isActive() && u.getRole() == Role.ADMIN)
          .forEach(u -> recipients.put(u.getId(), u));
    for (var u : recipients.values()) {
      if (u.isActive()) {
        inApp.deliver(u, i, e.action(),
            "INC-" + i.getId() + " · " + i.getTitle() + ": " + e.description());
      }
    }
  }
}
