package com.iitsm.exception;

public class ServiceNotFoundException extends AppException {
  public ServiceNotFoundException(String message) {
    super(org.springframework.http.HttpStatus.NOT_FOUND, message);
  }
}
