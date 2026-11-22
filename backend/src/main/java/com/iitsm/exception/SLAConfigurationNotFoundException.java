package com.iitsm.exception;

public class SLAConfigurationNotFoundException extends AppException {
  public SLAConfigurationNotFoundException(String message) {
    super(org.springframework.http.HttpStatus.CONFLICT, message);
  }
}
