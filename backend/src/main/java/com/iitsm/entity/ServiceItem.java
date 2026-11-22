package com.iitsm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItem extends CatalogItem {
  private int criticality;
  private String requiredSkill;

  public ServiceItem(String n, int c, String s) {
    super(n);
    configure(c, s);
  }

  public void configure(int c, String s) {
    if (c < 1 || c > 3) throw new IllegalArgumentException("Criticality must be 1–3");
    criticality = c;
    requiredSkill = s;
  }
}
