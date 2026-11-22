package com.iitsm.controller;

import com.iitsm.dto.*;
import com.iitsm.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService a) {
    auth = a;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Views.Auth register(@Valid @RequestBody Requests.Register r) {
    return auth.register(r);
  }

  @PostMapping("/login")
  public Views.Auth login(@Valid @RequestBody Requests.Login r) {
    return auth.login(r);
  }
}
