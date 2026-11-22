package com.iitsm.repository;

import com.iitsm.entity.Notification;
import org.springframework.data.jpa.repository.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  java.util.List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long id);
}
