package com.iitsm.entity;

import com.iitsm.model.Role;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {
  protected Employee() {}

  public Employee(String name, String email, String hash) {
    super(name, email, hash);
  }

  @Override
  public Role getRole() {
    return Role.EMPLOYEE;
  }
}
