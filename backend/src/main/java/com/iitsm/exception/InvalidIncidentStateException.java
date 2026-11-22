package com.iitsm.exception;

public class InvalidIncidentStateException extends AppException {
  public InvalidIncidentStateException(String message) {
    super(org.springframework.http.HttpStatus.CONFLICT, message);
  }
}
