package com.iitsm.exception;

public class UnauthorizedOperationException extends AppException {
  public UnauthorizedOperationException(String message) {
    super(org.springframework.http.HttpStatus.FORBIDDEN, message);
  }
}
