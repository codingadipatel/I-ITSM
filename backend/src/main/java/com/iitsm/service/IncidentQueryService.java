package com.iitsm.service;

import com.iitsm.dto.Views.*;
import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.model.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncidentQueryService {
  private final IncidentRepository incidents;
  private final IncidentNoteRepository notes;
  private final AuditLogRepository audits;
  private final DuplicateSuggestionRepository duplicates;
  private final FeedbackRepository feedback;
  private final AccessService access;
  private final ViewMapper mapper;

  public IncidentQueryService(
      IncidentRepository i,
      IncidentNoteRepository n,
      AuditLogRepository a,
      DuplicateSuggestionRepository d,
      FeedbackRepository f,
      AccessService s,
      ViewMapper m) {
    incidents = i;
    notes = n;
    audits = a;
    duplicates = d;
    feedback = f;
    access = s;
    mapper = m;
  }

  public Incident find(Long id) {
    var i =
        incidents
            .findById(id)
            .orElseThrow(() -> new IncidentNotFoundException("Incident " + id + " not found"));
    access.read(access.current(), i);
    return i;
  }

  public static Specification<Incident> visible(User u) {
    return (r, q, b) ->
        u.getRole() == Role.ADMIN
            ? b.conjunction()
            : u.getRole() == Role.EMPLOYEE
                ? b.equal(r.get("reporter").get("id"), u.getId())
                : b.or(
                    b.equal(r.get("technician").get("id"), u.getId()),
                    b.equal(r.get("reporter").get("id"), u.getId()));
  }

  private Specification<Incident> filters(Map<String, String> f) {
    Specification<Incident> spec = visible(access.current());
    for (String key :
        List.of(
            "status", "priority", "slaLevel", "serviceId", "categoryId", "technicianId", "id")) {
      String val = f.get(key);
      if (val == null || val.isBlank()) continue;
      Object checked =
          switch (key) {
            case "status" -> Status.valueOf(val);
            case "priority" -> Priority.valueOf(val);
            case "slaLevel" -> SlaLevel.valueOf(val);
            default -> Long.valueOf(val);
          };
      spec =
          spec.and(
              (r, q, b) ->
                  switch (key) {
                    case "status" -> b.equal(r.get("status"), Status.valueOf(val));
                    case "priority" -> b.equal(r.get("priority"), Priority.valueOf(val));
                    case "slaLevel" ->
                        b.equal(r.get("sla").get("warningLevel"), SlaLevel.valueOf(val));
                    case "serviceId" -> b.equal(r.get("service").get("id"), Long.valueOf(val));
                    case "categoryId" -> b.equal(r.get("category").get("id"), Long.valueOf(val));
                    case "technicianId" ->
                        b.equal(r.get("technician").get("id"), Long.valueOf(val));
                    default -> b.equal(r.get("id"), Long.valueOf(val));
                  });
    }
    String technicianName = f.get("technicianName");
    if (technicianName != null && !technicianName.isBlank()) {
      spec = spec.and((r,q,b) -> b.like(b.lower(r.get("technician").get("name")),
          "%" + technicianName.strip().toLowerCase(Locale.ROOT) + "%"));
    }
    if (f.containsKey("q") && !f.get("q").isBlank()) {
      String term = f.get("q").toLowerCase(Locale.ROOT);
      if (term.matches("(?i)(inc-)?[0-9]+")) {
        Long id = Long.valueOf(term.replace("inc-", ""));
        spec = spec.and((r, q, b) -> b.equal(r.get("id"), id));
      } else
        spec =
            spec.and(
                (r, q, b) ->
                    b.or(
                        b.like(b.lower(r.get("title")), "%" + term + "%"),
                        b.like(b.lower(r.get("description")), "%" + term + "%")));
    }
    if (f.containsKey("from") && !f.get("from").isBlank()) {
      Instant from = LocalDate.parse(f.get("from")).atStartOfDay(ZoneOffset.UTC).toInstant();
      spec = spec.and((r, q, b) -> b.greaterThanOrEqualTo(r.get("createdAt"), from));
    }
    if (f.containsKey("to") && !f.get("to").isBlank()) {
      Instant to =
          LocalDate.parse(f.get("to")).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
      spec = spec.and((r, q, b) -> b.lessThan(r.get("createdAt"), to));
    }
    return spec;
  }

  public List<IncidentView> export(Map<String, String> f) {
    return incidents.findAll(filters(f), Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        .stream().map(mapper::incident).toList();
  }

  public Page<IncidentView> search(Map<String, String> f, int page, int size) {
    return incidents
        .findAll(
            filters(f),
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")))
        .map(mapper::incident);
  }

  public DetailView detail(Long id) {
    var i = find(id);
    var actor = access.current();
    return new DetailView(
        mapper.incident(i),
        notes.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(mapper::note).toList(),
        audits.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(mapper::audit).toList(),
        actor.getRole() == Role.EMPLOYEE
            ? List.of()
            : duplicates.findByIncidentIdOrRelatedId(id, id).stream()
                .map(d -> mapper.duplicate(d, id))
                .toList(),
        feedback
            .findByIncidentId(id)
            .map(
                f ->
                    Map.<String, Object>of(
                        "rating", f.getRating(), "comment", Objects.toString(f.getComment(), "")))
            .orElse(Map.of()));
  }
}
