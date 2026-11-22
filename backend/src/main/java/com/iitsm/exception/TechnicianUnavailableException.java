package com.iitsm.exception;

public class TechnicianUnavailableException extends AppException {
  public TechnicianUnavailableException(String message) {
    super(org.springframework.http.HttpStatus.CONFLICT, message);
  }
}
