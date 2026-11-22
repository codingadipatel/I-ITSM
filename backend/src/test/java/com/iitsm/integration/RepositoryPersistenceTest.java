package com.iitsm.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.entity.*;
import com.iitsm.repository.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryPersistenceTest {
  @Autowired UserRepository users;
  @Autowired TechnicianRepository technicians;
  @Autowired SkillRepository skills;
  @Autowired org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager em;

  @Test
  void joinedSkillsAndSubtypeSurvivePersistenceContextClear() {
    var t = new Technician("T", "repo@test.local", "bcrypt-fixture-hash");
    t.configure(true, 4, 2, Set.of(skills.findAll().get(0)));
    t = technicians.saveAndFlush(t);
    long id = t.getId();
    em.clear();
    var loaded = technicians.findById(id).orElseThrow();
    assertEquals(1, loaded.getSkills().size());
    assertEquals(4, loaded.getMaxWorkload());
    assertEquals(com.iitsm.model.Role.TECHNICIAN, users.findById(id).orElseThrow().getRole());
  }

  @Test
  void caseInsensitiveLookupIsConsistent() {
    users.saveAndFlush(new Employee("E", "Repo2@Test.Local", "bcrypt-fixture-hash"));
    em.clear();
    assertTrue(users.findByEmailIgnoreCase("REPO2@test.local").isPresent());
  }
}
