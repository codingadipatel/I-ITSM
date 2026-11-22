package com.iitsm.controller;

import com.iitsm.dto.Requests;
import com.iitsm.dto.Views;
import com.iitsm.entity.*;
import com.iitsm.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigurationController {
  private final ConfigurationService config;
  private final IncidentQueryService incidents;

  public ConfigurationController(ConfigurationService c, IncidentQueryService i) {
    config = c;
    incidents = i;
  }

  @GetMapping("/settings")
  public SystemSettings settings() {
    return config.settings();
  }

  @PutMapping("/settings")
  public SystemSettings settings(@Valid @RequestBody Requests.Settings r) {
    return config.update(r);
  }

  @GetMapping("/sla/policies")
  public List<SlaPolicy> policies() {
    return config.policies();
  }

  @PostMapping("/sla/policies")
  public SlaPolicy create(@Valid @RequestBody Requests.Policy r) {
    return config.policy(null, r);
  }

  @PutMapping("/sla/policies/{id}")
  public SlaPolicy update(@PathVariable Long id, @Valid @RequestBody Requests.Policy r) {
    return config.policy(id, r);
  }

  @GetMapping("/sla/violations")
  public org.springframework.data.domain.Page<Views.IncidentView> violations(
      @RequestParam(defaultValue = "0") int page) {
    return incidents.search(Map.of("slaLevel", "VIOLATED"), page, 20);
  }
}
