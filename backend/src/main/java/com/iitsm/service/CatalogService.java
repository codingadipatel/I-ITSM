package com.iitsm.service;

import com.iitsm.dto.Requests;
import com.iitsm.entity.*;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
  private final ServiceItemRepository services;
  private final IncidentCategoryRepository categories;
  private final AccessService access;
  private final IncidentEventPublisher events;

  public CatalogService(
      ServiceItemRepository s,
      IncidentCategoryRepository c,
      AccessService a,
      IncidentEventPublisher e) {
    services = s;
    categories = c;
    access = a;
    events = e;
  }

  public ServiceItem service(Long id, Requests.CatalogInput r) {
    var actor = access.current();
    access.admin(actor);
    var s =
        id == null
            ? new ServiceItem(r.name(), r.criticality(), r.requiredSkill())
            : services
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
    s.update(r.name(), r.active());
    s.configure(r.criticality(), r.requiredSkill());
    services.save(s);
    events.publish(new IncidentEvent(null, actor, "SERVICE_CONFIGURED", null, null, s.getName()));
    return s;
  }

  public IncidentCategory category(Long id, Requests.CatalogInput r) {
    var actor = access.current();
    access.admin(actor);
    var c =
        id == null
            ? new IncidentCategory(r.name(), r.requiredSkill())
            : categories
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    c.update(r.name(), r.active());
    c.configure(r.requiredSkill());
    categories.save(c);
    events.publish(new IncidentEvent(null, actor, "CATEGORY_CONFIGURED", null, null, c.getName()));
    return c;
  }
}
