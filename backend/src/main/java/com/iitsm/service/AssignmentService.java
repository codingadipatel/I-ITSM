package com.iitsm.service;

import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.model.*;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.state.StateRegistry;
import com.iitsm.strategy.TechnicianAssignmentStrategy;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssignmentService {
  private final Map<String, TechnicianAssignmentStrategy> strategies = new HashMap<>();
  private final TechnicianRepository technicians;
  private final IncidentRepository incidents;
  private final AssignmentRepository assignments;
  private final ConfigurationService config;
  private final IncidentEventPublisher events;
  private final StateRegistry states;

  public AssignmentService(
      List<TechnicianAssignmentStrategy> all,
      TechnicianRepository t,
      IncidentRepository i,
      AssignmentRepository a,
      ConfigurationService c,
      IncidentEventPublisher e,
      StateRegistry s) {
    all.forEach(x -> strategies.put(x.key(), x));
    technicians = t;
    incidents = i;
    assignments = a;
    config = c;
    events = e;
    states = s;
  }

  public long workload(Long id) {
    return incidents.countByTechnicianIdAndStatusNotIn(
        id, List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED));
  }

  public Optional<TechnicianAssignmentStrategy.Recommendation> recommend(Incident i) {
    return strategies
        .get(config.settings().getAssignmentStrategy())
        .recommend(
            i,
            technicians.findAll().stream()
                .map(
                    t ->
                        new TechnicianAssignmentStrategy.Candidate(
                            t,
                            workload(t.getId()),
                            (int)
                                incidents.countByTechnicianIdAndCategoryIdAndStatusIn(
                                    t.getId(),
                                    i.getCategory().getId(),
                                    List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED))))
                .toList());
  }

  public void autoAssign(Incident i, User actor) {
    recommend(i)
        .ifPresent(
            r -> {
              events.publish(
                  new IncidentEvent(i, actor, "TECHNICIAN_RECOMMENDED", null, null, r.reason()));
              assign(i, r.technician().getId(), actor, r.reason());
            });
  }

  public void assign(Incident i, Long technicianId, User actor, String reason) {
    if (List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED).contains(i.getStatus()))
      throw new InvalidIncidentStateException("Reopen the incident before reassigning it");
    // Serialize capacity checks per technician to avoid concurrent over-allocation.
    var t =
        technicians
            .findLockedById(technicianId)
            .orElseThrow(() -> new TechnicianUnavailableException("Technician does not exist"));
    if (!t.isActive() || !t.isAvailable())
      throw new TechnicianUnavailableException("Technician is unavailable");
    if ((i.getTechnician() == null || !t.getId().equals(i.getTechnician().getId()))
        && workload(t.getId()) >= t.getMaxWorkload())
      throw new WorkloadExceededException("Technician has reached maximum workload");
    String before = i.getTechnician() == null ? "Unassigned" : i.getTechnician().getName();
    Status statusBefore = i.getStatus();
    i.assign(t, reason);
    if (i.getStatus() == Status.CLASSIFIED)
      i.transition(states.get(i.getStatus()), Status.ASSIGNED);
    assignments.save(new Assignment(i, t, actor, reason));
    events.publish(new IncidentEvent(i, actor, "ASSIGNED", before, t.getName(), reason,
        statusBefore, i.getStatus()));
  }
}
