package com.iitsm.service;

import com.iitsm.dto.Requests.*;
import com.iitsm.entity.*;
import com.iitsm.exception.*;
import com.iitsm.model.*;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import com.iitsm.state.StateRegistry;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncidentService {
  private final IncidentRepository incidents;
  private final ServiceItemRepository services;
  private final IncidentCategoryRepository categories;
  private final IncidentNoteRepository notes;
  private final FeedbackRepository feedback;
  private final DuplicateSuggestionRepository duplicates;
  private final AccessService access;
  private final PriorityCalculationService priority;
  private final AssignmentService assignment;
  private final DuplicateDetectionService detector;
  private final IncidentEventPublisher events;
  private final StateRegistry states;

  public IncidentService(
      IncidentRepository i,
      ServiceItemRepository s,
      IncidentCategoryRepository c,
      IncidentNoteRepository n,
      FeedbackRepository f,
      DuplicateSuggestionRepository d,
      AccessService a,
      PriorityCalculationService p,
      AssignmentService assign,
      DuplicateDetectionService detect,
      IncidentEventPublisher e,
      StateRegistry st) {
    incidents = i;
    services = s;
    categories = c;
    notes = n;
    feedback = f;
    duplicates = d;
    access = a;
    priority = p;
    assignment = assign;
    detector = detect;
    events = e;
    states = st;
  }

  private Incident locked(Long id) {
    var i =
        incidents
            .findLockedById(id)
            .orElseThrow(() -> new IncidentNotFoundException("Incident not found"));
    access.read(access.current(), i);
    return i;
  }

  private ServiceItem service(Long id) {
    return services
        .findById(id)
        .filter(ServiceItem::isActive)
        .orElseThrow(() -> new ServiceNotFoundException("Select an active service"));
  }

  private IncidentCategory category(Long id) {
    return categories
        .findById(id)
        .filter(IncidentCategory::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Select an active category"));
  }

  private void event(Incident i, User actor, String action, String before, String details) {
    events.publish(new IncidentEvent(i, actor, action, before, i.getStatus().name(), details));
  }

  public Incident create(IncidentInput r) {
    User actor = access.current();
    var i =
        incidents.save(
            new Incident(
                actor,
                r.title(),
                r.description(),
                service(r.serviceId()),
                category(r.categoryId()),
                r.impact(),
                r.urgency(),
                r.affectedUsers()));
    event(i, actor, "CREATED", null, "Incident reported");
    i.transition(states.get(i.getStatus()), Status.CLASSIFIED);
    events.publish(new IncidentEvent(i, actor, "CLASSIFIED", "NEW", "CLASSIFIED",
        "Service and category classified", Status.NEW, Status.CLASSIFIED));
    priority.calculate(i);
    event(i, actor, "PRIORITY_CALCULATED", null, i.getPriorityReason());
    detector.detect(i);
    assignment.autoAssign(i, actor);
    return i;
  }

  public Incident update(Long id, IncidentInput r) {
    var i = locked(id);
    var actor = access.current();
    if (actor.getRole() != Role.ADMIN) {
      access.reporter(actor, i);
      if (!List.of(Status.CLASSIFIED, Status.ASSIGNED).contains(i.getStatus()))
        throw new InvalidIncidentStateException(
            "Add a note after acknowledgement; only an administrator may revise classification");
    }
    if (List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED).contains(i.getStatus()))
      throw new InvalidIncidentStateException("Cannot edit a resolved incident");
    String old = i.getPriority().name();
    i.revise(
        r.title(),
        r.description(),
        service(r.serviceId()),
        category(r.categoryId()),
        r.impact(),
        r.urgency(),
        r.affectedUsers());
    priority.calculate(i);
    events.publish(
        new IncidentEvent(
            i, actor, "PRIORITY_UPDATED", old, i.getPriority().name(), i.getPriorityReason()));
    return i;
  }

  public Incident assign(Long id, Assign r) {
    var actor = access.current();
    access.admin(actor);
    var i = locked(id);
    assignment.assign(i, r.technicianId(), actor, r.reason());
    return i;
  }

  public Incident transition(Long id, String action, String details) {
    var i = locked(id);
    var actor = access.current();
    Status before = i.getStatus();
    Status target =
        switch (action) {
          case "acknowledge" -> Status.ACKNOWLEDGED;
          case "investigate", "reopen" -> Status.IN_PROGRESS;
          case "resolve" -> Status.RESOLVED;
          case "verify" -> Status.VERIFIED;
          case "close" -> Status.CLOSED;
          default -> throw new IllegalArgumentException("Unknown action");
        };
    if (action.equals("verify") || action.equals("close")) access.reporter(actor, i);
    else if (action.equals("reopen")) {
      if (!actor.getId().equals(i.getReporter().getId())) access.technician(actor, i);
      if (!List.of(Status.RESOLVED, Status.VERIFIED, Status.CLOSED).contains(i.getStatus()))
        throw new InvalidIncidentStateException(
            "Only a resolved, verified or closed incident can be reopened");
      if (details == null || details.trim().length() < 10)
        throw new IllegalArgumentException("Provide at least 10 characters justifying reopening");
    } else access.technician(actor, i);
    if (action.equals("resolve") && (details == null || details.trim().length() < 10))
      throw new IllegalArgumentException("Provide at least 10 characters of resolution details");
    Instant now = Instant.now();
    boolean previouslyViolated =
        i.getSla().isResponseViolated() || i.getSla().isResolutionViolated();
    if (action.equals("resolve")) i.resolve(details, now);
    i.transition(states.get(i.getStatus()), target);
    switch (action) {
      case "acknowledge" -> i.acknowledge(now);
      case "resolve" -> {}
      case "verify" -> i.verify(details, now);
      case "close" -> i.close(now);
      case "reopen" -> i.reopen();
      default -> {}
    }
    events.publish(new IncidentEvent(i, actor, action.toUpperCase(Locale.ROOT), before.name(),
        target.name(), details == null ? action : details, before, target));
    if (!previouslyViolated
        && (i.getSla().isResponseViolated() || i.getSla().isResolutionViolated()))
      event(i, null, "SLA_VIOLATED_ESCALATION", null, "SLA deadline breached; manager notified");
    return i;
  }

  public void note(Long id, Detail r, boolean request) {
    var i = locked(id);
    var actor = access.current();
    if (i.getStatus() == Status.CLOSED)
      throw new InvalidIncidentStateException("Reopen before adding information");
    if (request) access.technician(actor, i);
    notes.save(new IncidentNote(i, actor, r.details(), request));
    i.touch();
    event(i, actor, request ? "INFORMATION_REQUESTED" : "NOTE_ADDED", null, r.details());
  }

  public void feedback(Long id, Rating r) {
    var i = locked(id);
    var actor = access.current();
    access.reporter(actor, i);
    if (!List.of(Status.VERIFIED, Status.CLOSED).contains(i.getStatus()))
      throw new InvalidIncidentStateException("Verify the resolution before providing feedback");
    if (feedback.findByIncidentId(id).isPresent())
      throw new InvalidIncidentStateException("Feedback has already been submitted");
    feedback.save(new Feedback(i, r.rating(), r.comment()));
    event(i, actor, "FEEDBACK_ADDED", null, "Rating: " + r.rating());
  }

  public void review(Long id, Long suggestionId, String decision) {
    var i = locked(id);
    var actor = access.current();
    access.technician(actor, i);
    var d =
        duplicates
            .findById(suggestionId)
            .orElseThrow(() -> new IllegalArgumentException("Suggestion not found"));
    if (!d.getIncident().getId().equals(id) && !d.getRelated().getId().equals(id))
      throw new IllegalArgumentException("Suggestion does not belong to this incident");
    d.review(actor, decision);
    event(
        i,
        actor,
        "DUPLICATE_" + decision,
        null,
        "Suggestion " + suggestionId + " reviewed; all records retained");
  }
}
