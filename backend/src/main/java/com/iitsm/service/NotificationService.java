package com.iitsm.service;

import com.iitsm.dto.Requests.ChatMessage;
import com.iitsm.dto.Views;
import com.iitsm.entity.Notification;
import com.iitsm.entity.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.repository.NotificationRepository;
import com.iitsm.security.AccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
  private final NotificationRepository notifications;
  private final AccessService access;
  private final ViewMapper mapper;
  private final IncidentChatService chat;

  public NotificationService(NotificationRepository n, AccessService a, ViewMapper m, IncidentChatService chat) {
    notifications = n;
    access = a;
    mapper = m;
    this.chat = chat;
  }

  @Transactional(readOnly = true)
  public List<Views.NotificationView> list() {
    var actor = access.current();
    return notifications.findByRecipientIdOrderByCreatedAtDesc(actor.getId()).stream()
        .map(n -> mapper.notification(n, chat.conversation(n.getIncident(), actor)))
        .toList();
  }

  public void read(Long id) {
    ownedNotification(id, access.current()).markRead();
  }

  public void reply(Long id, ChatMessage message) {
    var n = ownedNotification(id, access.current());
    if (n.getIncident() == null)
      throw new IllegalArgumentException("This notification has no incident to reply to");
    chat.send(n.getIncident().getId(), message.technicianId(), message.body(), n.getId());
    n.markRead();
  }

  private Notification ownedNotification(Long id, User actor) {
    var n =
        notifications
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    if (!n.getRecipient().getId().equals(actor.getId()))
      throw new UnauthorizedOperationException("This notification belongs to another user");
    return n;
  }
}
