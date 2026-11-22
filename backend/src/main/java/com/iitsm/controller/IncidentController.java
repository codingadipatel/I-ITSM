package com.iitsm.controller;

import com.iitsm.dto.*;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
  private final IncidentService service;
  private final IncidentQueryService queries;
  private final AssignmentService assignments;
  private final ViewMapper mapper;

  public IncidentController(
      IncidentService s, IncidentQueryService q, AssignmentService a, ViewMapper m) {
    service = s;
    queries = q;
    assignments = a;
    mapper = m;
  }

  @GetMapping
  public Page<Views.IncidentView> list(
      @RequestParam Map<String, String> f,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return queries.search(f, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Views.IncidentView create(@Valid @RequestBody Requests.IncidentInput r) {
    return mapper.incident(service.create(r));
  }

  @GetMapping("/export")
  public List<Views.IncidentView> export(@RequestParam Map<String, String> filters) {
    return queries.export(filters);
  }

  @GetMapping("/{id}")
  public Views.DetailView get(@PathVariable Long id) {
    return queries.detail(id);
  }

  @PutMapping("/{id}")
  public Views.IncidentView update(
      @PathVariable Long id, @Valid @RequestBody Requests.IncidentInput r) {
    return mapper.incident(service.update(id, r));
  }

  @PostMapping("/{id}/assign")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.IncidentView assign(@PathVariable Long id, @Valid @RequestBody Requests.Assign r) {
    return mapper.incident(service.assign(id, r));
  }

  @GetMapping("/{id}/recommendations")
  @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
  public List<Views.Recommendation> recommendations(@PathVariable Long id) {
    return assignments.recommend(queries.find(id)).stream()
        .map(
            r ->
                new Views.Recommendation(
                    r.technician().getId(), r.technician().getName(), r.reason(), r.score()))
        .toList();
  }

  @PostMapping("/{id}/acknowledge")
  public Views.IncidentView ack(@PathVariable Long id) {
    return mapper.incident(service.transition(id, "acknowledge", null));
  }

  @PostMapping("/{id}/investigate")
  public Views.IncidentView investigate(@PathVariable Long id) {
    return mapper.incident(service.transition(id, "investigate", null));
  }

  @PostMapping("/{id}/resolve")
  public Views.IncidentView resolve(@PathVariable Long id, @Valid @RequestBody Requests.Detail r) {
    return mapper.incident(service.transition(id, "resolve", r.details()));
  }

  @PostMapping("/{id}/verify")
  public Views.IncidentView verify(@PathVariable Long id, @Valid @RequestBody Requests.Detail r) {
    return mapper.incident(service.transition(id, "verify", r.details()));
  }

  @PostMapping("/{id}/close")
  public Views.IncidentView close(@PathVariable Long id) {
    return mapper.incident(service.transition(id, "close", null));
  }

  @PostMapping("/{id}/reopen")
  public Views.IncidentView reopen(@PathVariable Long id, @Valid @RequestBody Requests.Detail r) {
    return mapper.incident(service.transition(id, "reopen", r.details()));
  }

  @PostMapping("/{id}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Boolean> note(@PathVariable Long id, @Valid @RequestBody Requests.Detail r) {
    service.note(id, r, false);
    return Map.of("saved", true);
  }

  @PostMapping("/{id}/request-information")
  @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
  public Map<String, Boolean> request(
      @PathVariable Long id, @Valid @RequestBody Requests.Detail r) {
    service.note(id, r, true);
    return Map.of("saved", true);
  }

  @PostMapping("/{id}/feedback")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Boolean> feedback(
      @PathVariable Long id, @Valid @RequestBody Requests.Rating r) {
    service.feedback(id, r);
    return Map.of("saved", true);
  }

  @PostMapping("/{id}/duplicates/{suggestionId}")
  @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
  public Map<String, Boolean> duplicate(
      @PathVariable Long id,
      @PathVariable Long suggestionId,
      @Valid @RequestBody Requests.Decision r) {
    service.review(id, suggestionId, r.decision());
    return Map.of("saved", true);
  }
}
