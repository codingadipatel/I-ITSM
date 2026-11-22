package com.iitsm.service;

import com.iitsm.dto.Requests;
import com.iitsm.entity.*;
import com.iitsm.factory.UserFactory;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.security.AccessService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserManagementService {
  private final UserRepository users;
  private final SkillRepository skills;
  private final UserFactory factory;
  private final AccessService access;
  private final IncidentEventPublisher events;

  public UserManagementService(
      UserRepository u,
      SkillRepository s,
      UserFactory f,
      AccessService a,
      IncidentEventPublisher e) {
    users = u;
    skills = s;
    factory = f;
    access = a;
    events = e;
  }

  private Set<Skill> skills(Set<String> names) {
    Set<Skill> result = new HashSet<>();
    names.forEach(
        n ->
            result.add(
                skills
                    .findByNameIgnoreCase(n.trim())
                    .orElseGet(() -> skills.save(new Skill(n.trim())))));
    return result;
  }

  public User save(Long id, Requests.UserInput r) {
    var actor = access.current();
    access.admin(actor);
    User u =
        id == null
            ? factory.create(r.role(), r.name(), r.email(), r.password())
            : users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (id != null && (u.getRole() != r.role() || !u.getEmail().equalsIgnoreCase(r.email())))
      throw new IllegalArgumentException(
          "Role and email are immutable; create a new account instead");
    if (u.getId() != null && u.getId().equals(actor.getId()) && !r.active())
      throw new IllegalArgumentException("Cannot deactivate your own account");
    u.update(r.name(), r.active());
    if (u instanceof Technician t)
      t.configure(r.available(), r.maxWorkload(), r.experienceYears(), skills(r.skills()));
    users.save(u);
    events.publish(
        new IncidentEvent(
            null,
            actor,
            id == null ? "USER_CREATED" : "USER_UPDATED",
            null,
            null,
            "Managed account " + u.getEmail()));
    return u;
  }

  public void deactivate(Long id) {
    var actor = access.current();
    access.admin(actor);
    if (id.equals(actor.getId()))
      throw new IllegalArgumentException("Cannot deactivate your own account");
    var u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    u.update(u.getName(), false);
    events.publish(new IncidentEvent(null, actor, "USER_DEACTIVATED", null, null, u.getEmail()));
  }

  public User profile(Requests.Profile r) {
    var u = access.current();
    u.update(r.name(), u.isActive());
    return u;
  }
}
