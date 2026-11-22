package com.iitsm.exception;

public class WorkloadExceededException extends AppException {
  public WorkloadExceededException(String message) {
    super(org.springframework.http.HttpStatus.CONFLICT, message);
  }
}
