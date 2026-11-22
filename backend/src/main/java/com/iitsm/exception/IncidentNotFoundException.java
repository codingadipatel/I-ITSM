package com.iitsm.exception;

public class IncidentNotFoundException extends AppException {
  public IncidentNotFoundException(String message) {
    super(org.springframework.http.HttpStatus.NOT_FOUND, message);
  }
}
