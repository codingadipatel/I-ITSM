package com.iitsm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "incident_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentCategory extends CatalogItem {
  private String requiredSkill;

  public IncidentCategory(String n, String s) {
    super(n);
    requiredSkill = s;
  }

  public void configure(String skill) {
    requiredSkill = skill;
  }
}
