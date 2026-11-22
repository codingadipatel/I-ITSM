package com.iitsm.service;

import com.iitsm.dto.*;
import com.iitsm.exception.AppException;
import com.iitsm.factory.UserFactory;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.model.Role;
import com.iitsm.repository.*;
import com.iitsm.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  private final UserRepository users;
  private final UserFactory factory;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final ViewMapper mapper;

  public AuthService(UserRepository u, UserFactory f, PasswordEncoder e, JwtService j, ViewMapper m) {
    users = u;
    factory = f;
    encoder = e;
    jwt = j;
    mapper = m;
  }

  public Views.Auth register(Requests.Register r) {
    if (users.existsByEmailIgnoreCase(r.email()))
      throw new AppException(HttpStatus.CONFLICT, "Email is already registered");
    var user = users.save(factory.create(Role.EMPLOYEE, r.name(), r.email(), r.password()));
    return new Views.Auth(jwt.issue(user.getEmail()), mapper.user(user));
  }

  public Views.Auth login(Requests.Login r) {
    var u =
        users
            .findByEmailIgnoreCase(r.email())
            .filter(x -> x.isActive() && encoder.matches(r.password(), x.getPasswordHash()))
            .orElseThrow(
                () -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    return new Views.Auth(jwt.issue(u.getEmail()), mapper.user(u));
  }
}
