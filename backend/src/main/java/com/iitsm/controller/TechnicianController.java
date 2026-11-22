package com.iitsm.controller;

import com.iitsm.dto.Views;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.model.Role;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import com.iitsm.service.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technicians")
@PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
public class TechnicianController {
  private final TechnicianRepository technicians;
  private final AccessService access;
  private final AssignmentService assignments;
  private final IncidentQueryService queries;
  private final ViewMapper mapper;

  public TechnicianController(
      TechnicianRepository t,
      AccessService a,
      AssignmentService s,
      IncidentQueryService q,
      ViewMapper m) {
    technicians = t;
    access = a;
    assignments = s;
    queries = q;
    mapper = m;
  }

  @GetMapping
  public List<Views.UserView> list() {
    var u = access.current();
    return technicians.findAll().stream()
        .filter(t -> u.getRole() == Role.ADMIN || t.getId().equals(u.getId()))
        .map(mapper::user)
        .toList();
  }

  @GetMapping("/{id}/workload")
  public Map<String, Object> workload(@PathVariable Long id) {
    var u = access.current();
    if (u.getRole() != Role.ADMIN && !u.getId().equals(id))
      throw new UnauthorizedOperationException("You may only see your workload");
    var t =
        technicians
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Technician not found"));
    return Map.of("technician", mapper.user(t), "active", assignments.workload(id));
  }

  @GetMapping("/{id}/recommendations")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Views.Recommendation> recommendation(
      @PathVariable Long id, @RequestParam Long incidentId) {
    return assignments
        .recommend(queries.find(incidentId))
        .filter(r -> r.technician().getId().equals(id))
        .stream()
        .map(
            r ->
                new Views.Recommendation(
                    r.technician().getId(), r.technician().getName(), r.reason(), r.score()))
        .toList();
  }
}
