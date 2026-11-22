package com.iitsm.config;

import com.iitsm.entity.*;
import com.iitsm.factory.UserFactory;
import com.iitsm.model.*;
import com.iitsm.observer.*;
import com.iitsm.repository.*;
import com.iitsm.service.*;
import com.iitsm.state.StateRegistry;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {
  private final boolean enabled;
  private final String password;
  private final UserRepository users;
  private final UserFactory factory;
  private final SkillRepository skills;
  private final IncidentRepository incidents;
  private final ServiceItemRepository services;
  private final IncidentCategoryRepository categories;
  private final PriorityCalculationService priorities;
  private final AssignmentService assignment;
  private final IncidentEventPublisher events;
  private final StateRegistry states;

  public DemoDataInitializer(
      @Value("${app.demo-enabled}") boolean e,
      @Value("${app.demo-password}") String p,
      UserRepository u,
      UserFactory f,
      SkillRepository sk,
      IncidentRepository i,
      ServiceItemRepository s,
      IncidentCategoryRepository c,
      PriorityCalculationService pr,
      AssignmentService a,
      IncidentEventPublisher ev,
      StateRegistry st) {
    enabled = e;
    password = p;
    users = u;
    factory = f;
    skills = sk;
    incidents = i;
    services = s;
    categories = c;
    priorities = pr;
    assignment = a;
    events = ev;
    states = st;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled || users.count() > 0) return;
    if (password.length() < 10)
      throw new IllegalStateException("Set DEMO_PASSWORD of at least 10 characters");
    var admin =
        users.save(factory.create(Role.ADMIN, "Alex Morgan", "admin@iitsm.local", password));
    var employee =
        users.save(factory.create(Role.EMPLOYEE, "Jordan Lee", "employee@iitsm.local", password));
    users.save(factory.create(Role.EMPLOYEE, "Sam Rivera", "sam@iitsm.local", password));
    var tech =
        (Technician)
            factory.create(Role.TECHNICIAN, "Priya Sharma", "technician@iitsm.local", password);
    tech.configure(true, 12, 6, new HashSet<>(skills.findAll()));
    users.save(tech);
    var second =
        (Technician) factory.create(Role.TECHNICIAN, "Marcus Chen", "marcus@iitsm.local", password);
    second.configure(
        true,
        8,
        4,
        new HashSet<>(
            skills.findAll().stream()
                .filter(s -> Set.of("Network", "Hardware", "Cloud").contains(s.getName()))
                .toList()));
    users.save(second);
    String[][] samples = {
      {
        "Payment gateway returning errors",
        "Customers receive timeout errors when completing payments on the checkout page."
      },
      {
        "VPN connection drops repeatedly",
        "The corporate VPN connection disconnects every few minutes and blocks remote work."
      },
      {
        "Unable to access employee portal",
        "Sign in fails with an authentication error even after resetting the account password."
      },
      {
        "Shared drive is unavailable",
        "Team members cannot open documents in the shared cloud storage workspace."
      },
      {
        "Laptop display flickering",
        "My workstation display flickers when connected to the external monitor."
      },
      {
        "Database queries timing out",
        "Analytics database queries take over a minute and frequently return connection timeout"
            + " errors."
      }
    };
    var sv = services.findAll();
    var cats = categories.findAll();
    int[] catIndices = {0, 2, 4, 5, 3, 1};
    for (int n = 0; n < samples.length; n++) {
      var i =
          incidents.save(
              new Incident(
                  employee,
                  samples[n][0],
                  samples[n][1],
                  sv.get(n),
                  cats.get(catIndices[n]),
                  n == 4 ? 1 : 3,
                  n == 4 ? 1 : 2,
                  n == 0 ? 150 : 12));
      events.publish(
          new IncidentEvent(
              i,
              employee,
              "CREATED",
              null,
              "NEW",
              "Demo incident reported through domain services"));
      i.transition(states.get(i.getStatus()), Status.CLASSIFIED);
      priorities.calculate(i);
      assignment.autoAssign(i, admin);
    }
  }
}
