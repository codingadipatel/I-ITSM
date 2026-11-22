package com.iitsm.controller;

import com.iitsm.dto.*;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.model.Role;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import com.iitsm.service.CatalogService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final ServiceItemRepository services;
  private final IncidentCategoryRepository categories;
  private final CatalogService service;
  private final AccessService access;
  private final ViewMapper mapper;

  public CatalogController(
      ServiceItemRepository s,
      IncidentCategoryRepository c,
      CatalogService service,
      AccessService a,
      ViewMapper m) {
    services = s;
    categories = c;
    this.service = service;
    access = a;
    mapper = m;
  }

  @GetMapping("/services")
  public List<Views.Catalog> services() {
    boolean admin = access.current().getRole() == Role.ADMIN;
    return services.findAll().stream()
        .filter(s -> admin || s.isActive())
        .map(mapper::service)
        .toList();
  }

  @GetMapping("/categories")
  public List<Views.Catalog> categories() {
    boolean admin = access.current().getRole() == Role.ADMIN;
    return categories.findAll().stream()
        .filter(c -> admin || c.isActive())
        .map(mapper::category)
        .toList();
  }

  @PostMapping("/services")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.Catalog createService(@Valid @RequestBody Requests.CatalogInput r) {
    return mapper.service(service.service(null, r));
  }

  @PutMapping("/services/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.Catalog service(@PathVariable Long id, @Valid @RequestBody Requests.CatalogInput r) {
    return mapper.service(service.service(id, r));
  }

  @PostMapping("/categories")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.Catalog createCategory(@Valid @RequestBody Requests.CatalogInput r) {
    return mapper.category(service.category(null, r));
  }

  @PutMapping("/categories/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.Catalog category(
      @PathVariable Long id, @Valid @RequestBody Requests.CatalogInput r) {
    return mapper.category(service.category(id, r));
  }
}
