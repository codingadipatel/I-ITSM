package com.iitsm.entity;

import com.iitsm.model.Role;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String name;

  private boolean active = true;
  private Instant createdAt = Instant.now();

  protected User(String name, String email, String hash) {
    this.name = name;
    this.email = email.toLowerCase();
    this.passwordHash = hash;
  }

  @Transient
  public abstract Role getRole();

  public void update(String name, boolean active) {
    this.name = name;
    this.active = active;
  }
}
