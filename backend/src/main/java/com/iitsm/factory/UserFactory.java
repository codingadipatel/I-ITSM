package com.iitsm.factory;

import com.iitsm.entity.*;
import com.iitsm.model.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
  private final PasswordEncoder encoder;

  public UserFactory(PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  public User create(Role role, String name, String email, String password) {
    if (password == null
        || password.length() < 10
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Password must be at least 10 characters and at most 72 UTF-8 bytes");
    String hash = encoder.encode(password);
    return switch (role) {
      case EMPLOYEE -> new Employee(name, email, hash);
      case TECHNICIAN -> new Technician(name, email, hash);
      case ADMIN -> new Administrator(name, email, hash);
    };
  }
}
