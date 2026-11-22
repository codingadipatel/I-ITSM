package com.iitsm.entity;

import com.iitsm.model.Role;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Entity
@DiscriminatorValue("TECHNICIAN")
@Getter
public class Technician extends User {
  private boolean available = true;
  private int maxWorkload = 8;
  private int experienceYears;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "technician_skills",
      joinColumns = @JoinColumn(name = "technician_id"),
      inverseJoinColumns = @JoinColumn(name = "skill_id"))
  private Set<Skill> skills = new HashSet<>();

  protected Technician() {}

  public Technician(String name, String email, String hash) {
    super(name, email, hash);
  }

  @Override
  public Role getRole() {
    return Role.TECHNICIAN;
  }

  public void configure(boolean available, int maximum, int experience, Set<Skill> skills) {
    if (maximum < 1 || maximum > 100 || experience < 0 || experience > 60)
      throw new IllegalArgumentException("Invalid technician capacity or experience");
    this.available = available;
    this.maxWorkload = maximum;
    this.experienceYears = experience;
    this.skills = new HashSet<>(skills);
  }

  public Set<Skill> getSkills() {
    return Collections.unmodifiableSet(skills);
  }
}
