package com.iitsm.entity;

import jakarta.persistence.*;
import lombok.*;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CatalogItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  private boolean active = true;

  protected CatalogItem(String name) {
    this.name = name;
  }

  public void update(String name, boolean active) {
    this.name = name;
    this.active = active;
  }
}
