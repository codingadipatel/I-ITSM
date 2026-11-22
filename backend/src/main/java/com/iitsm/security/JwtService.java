package com.iitsm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;

  public JwtService(@Value("${app.jwt-secret}") String secret) {
    if (secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String issue(String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(email)
        .issuer("i-itsm")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(28800)))
        .signWith(key)
        .compact();
  }

  public String subject(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer("i-itsm")
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
