package com.iitsm.observer;

import com.iitsm.entity.*;

public interface NotificationChannel {
  void deliver(User recipient, Incident incident, String type, String message);
}
