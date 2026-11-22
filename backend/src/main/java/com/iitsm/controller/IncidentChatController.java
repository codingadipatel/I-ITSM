package com.iitsm.controller;

import com.iitsm.dto.Requests.ChatMessage;
import com.iitsm.service.IncidentChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{id}/chat")
public class IncidentChatController {
  private final IncidentChatService chat;
  public IncidentChatController(IncidentChatService chat) { this.chat = chat; }

  @GetMapping
  public List<IncidentChatService.Message> messages(@PathVariable long id, @RequestParam long technicianId) { return chat.messages(id, technicianId); }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void send(@PathVariable long id, @Valid @RequestBody ChatMessage request) {
    chat.send(id, request.technicianId(), request.body(), null);
  }
}
