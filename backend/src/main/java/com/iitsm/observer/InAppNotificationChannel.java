package com.iitsm.observer;

import com.iitsm.entity.*;
import com.iitsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannel implements NotificationChannel {
  private final NotificationRepository repository;

  public InAppNotificationChannel(NotificationRepository repository) {
    this.repository = repository;
  }

  public void deliver(User u, Incident i, String t, String m) {
    repository.save(new Notification(u, i, t, m));
  }
}
