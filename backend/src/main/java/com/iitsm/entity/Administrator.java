package com.iitsm.entity;

import com.iitsm.model.Role;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {
  protected Administrator() {}

  public Administrator(String name, String email, String hash) {
    super(name, email, hash);
  }

  @Override
  public Role getRole() {
    return Role.ADMIN;
  }
}
