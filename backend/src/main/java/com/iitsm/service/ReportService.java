package com.iitsm.service;

import com.iitsm.entity.*;
import com.iitsm.model.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
  private final IncidentRepository incidents;
  private final TechnicianRepository technicians;
  private final AccessService access;
  private final AssignmentService assignments;

  public ReportService(
      IncidentRepository i, TechnicianRepository t, AccessService a, AssignmentService s) {
    incidents = i;
    technicians = t;
    access = a;
    assignments = s;
  }

  private List<Incident> visible() {
    return incidents.findAll(IncidentQueryService.visible(access.current()));
  }

  public record Counts(long previous, long selected) {
    Counts add(boolean current) {
      return new Counts(previous + (current ? 0 : 1), selected + (current ? 1 : 0));
    }
  }
  public record TechnicianCounts(Long id, String name, Counts counts) {}
  public record Comparison(LocalDate previousStart, LocalDate selectedStart, LocalDate endExclusive,
                           Counts total, List<TechnicianCounts> technicians) {}

  public Comparison comparison(String period, LocalDate date) {
    var unit = switch (period) {
      case "week" -> java.time.temporal.ChronoUnit.WEEKS;
      case "month" -> java.time.temporal.ChronoUnit.MONTHS;
      case "year" -> java.time.temporal.ChronoUnit.YEARS;
      default -> throw new IllegalArgumentException("Choose week, month or year");
    };
    LocalDate start = switch (period) {
      case "week" -> date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case "month" -> date.withDayOfMonth(1);
      default -> date.withDayOfYear(1);
    };
    var previous = start.minus(1, unit);
    var end = start.plus(1, unit);
    var scope = IncidentQueryService.visible(access.current()).and((r,q,b) ->
        b.and(b.greaterThanOrEqualTo(r.get("createdAt"), previous.atStartOfDay(ZoneOffset.UTC).toInstant()),
              b.lessThan(r.get("createdAt"), end.atStartOfDay(ZoneOffset.UTC).toInstant())));
    var grouped = new HashMap<Long, TechnicianCounts>();
    var total = new Counts(0, 0);
    for (var incident : incidents.findAll(scope)) {
      boolean current = !incident.getCreatedAt().isBefore(start.atStartOfDay(ZoneOffset.UTC).toInstant());
      var tech = incident.getTechnician();
      Long id = tech == null ? null : tech.getId();
      String name = tech == null ? "Unassigned" : tech.getName();
      var row = grouped.getOrDefault(id, new TechnicianCounts(id, name, new Counts(0, 0)));
      grouped.put(id, new TechnicianCounts(id, name, row.counts().add(current)));
      total = total.add(current);
    }
    return new Comparison(previous, start, end, total, grouped.values().stream()
        .sorted(Comparator.comparing(TechnicianCounts::name)
            .thenComparing(TechnicianCounts::id, Comparator.nullsLast(Comparator.naturalOrder()))).toList());
  }

  public Map<String, Object> overview() {
    var all = visible();
    long resolved = all.stream().filter(i -> i.getSla().getResolvedAt() != null).count();
    long ack = all.stream().filter(i -> i.getSla().getAcknowledgedAt() != null).count();
    long violations =
        all.stream()
            .filter(i -> i.getSla().isResponseViolated() || i.getSla().isResolutionViolated())
            .count();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("total", all.size());
    out.put(
        "open",
        all.stream()
            .filter(
                i ->
                    !List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED)
                        .contains(i.getStatus()))
            .count());
    out.put(
        "critical",
        all.stream()
            .filter(i -> i.getPriority() == Priority.P1 && i.getStatus() != Status.CLOSED)
            .count());
    out.put("resolved", resolved);
    out.put("closed", all.stream().filter(i -> i.getStatus() == Status.CLOSED).count());
    out.put("slaViolations", violations);
    out.put(
        "slaCompliance",
        all.isEmpty() ? 100 : Math.round((all.size() - violations) * 1000.0 / all.size()) / 10.0);
    out.put(
        "avgResponseMinutes",
        ack == 0
            ? 0
            : Math.round(
                all.stream()
                    .filter(i -> i.getSla().getAcknowledgedAt() != null)
                    .mapToLong(
                        i ->
                            Duration.between(i.getCreatedAt(), i.getSla().getAcknowledgedAt())
                                .toMinutes())
                    .average()
                    .orElse(0)));
    out.put(
        "avgResolutionMinutes",
        resolved == 0
            ? 0
            : Math.round(
                all.stream()
                    .filter(i -> i.getSla().getResolvedAt() != null)
                    .mapToLong(
                        i ->
                            Duration.between(i.getCreatedAt(), i.getSla().getResolvedAt())
                                .toMinutes())
                    .average()
                    .orElse(0)));
    out.put("byPriority", group(all, i -> i.getPriority().name()));
    return out;
  }

  private List<Map<String, Object>> group(List<Incident> list, Function<Incident, String> key) {
    return list.stream()
        .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()))
        .entrySet()
        .stream()
        .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
        .toList();
  }

  public List<Map<String, Object>> category() {
    return group(visible(), i -> i.getCategory().getName());
  }

  public List<Map<String, Object>> service() {
    return group(visible(), i -> i.getService().getName());
  }

  public List<Map<String, Object>> trends() {
    var all = visible();
    var counts =
        all.stream()
            .collect(
                Collectors.groupingBy(
                    i -> i.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    Collectors.counting()));
    List<Map<String, Object>> out = new ArrayList<>();
    for (int day = 13; day >= 0; day--) {
      LocalDate date = LocalDate.now(ZoneOffset.UTC).minusDays(day);
      out.add(Map.of("name", date.toString(), "value", counts.getOrDefault(date, 0L)));
    }
    return out;
  }

  public List<Map<String, Object>> workload() {
    var actor = access.current();
    if (actor.getRole() == Role.EMPLOYEE)
      throw new com.iitsm.exception.UnauthorizedOperationException("Technician access required");
    return technicians.findAll().stream()
        .filter(t -> actor.getRole() == Role.ADMIN || t.getId().equals(actor.getId()))
        .map(
            t ->
                Map.<String, Object>of(
                    "id",
                    t.getId(),
                    "name",
                    t.getName(),
                    "value",
                    assignments.workload(t.getId()),
                    "capacity",
                    t.getMaxWorkload(),
                    "available",
                    t.isActive() && t.isAvailable()))
        .toList();
  }
}
