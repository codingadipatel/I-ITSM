package com.iitsm.controller;

import com.iitsm.dto.Requests.ChatMessage;
import com.iitsm.dto.Views;
import com.iitsm.service.NotificationService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notifications;

  public NotificationController(NotificationService n) {
    notifications = n;
  }

  @GetMapping
  public List<Views.NotificationView> list() {
    return notifications.list();
  }

  @PostMapping("/{id}/reply")
  @ResponseStatus(HttpStatus.CREATED)
  public void reply(@PathVariable Long id, @Valid @RequestBody ChatMessage message) {
    notifications.reply(id, message);
  }

  @PatchMapping("/{id}/read")
  public Map<String, Boolean> read(@PathVariable Long id) {
    notifications.read(id);
    return Map.of("read", true);
  }
}
