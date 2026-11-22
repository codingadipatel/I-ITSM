package com.iitsm.controller;

import com.iitsm.dto.*;
import com.iitsm.mapper.ViewMapper;
import com.iitsm.repository.UserRepository;
import com.iitsm.security.AccessService;
import com.iitsm.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserRepository users;
  private final UserManagementService service;
  private final AccessService access;
  private final ViewMapper mapper;

  public UserController(UserRepository u, UserManagementService s, AccessService a, ViewMapper m) {
    users = u;
    service = s;
    access = a;
    mapper = m;
  }

  @GetMapping("/me")
  public Views.UserView me() {
    return mapper.user(access.current());
  }

  @PutMapping("/me")
  public Views.UserView profile(@Valid @RequestBody Requests.Profile r) {
    return mapper.user(service.profile(r));
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<Views.UserView> list() {
    return users.findAll().stream().map(mapper::user).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Views.UserView create(@Valid @RequestBody Requests.UserInput r) {
    return mapper.user(service.save(null, r));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Views.UserView update(@PathVariable Long id, @Valid @RequestBody Requests.UserInput r) {
    return mapper.user(service.save(id, r));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable Long id) {
    service.deactivate(id);
  }
}
