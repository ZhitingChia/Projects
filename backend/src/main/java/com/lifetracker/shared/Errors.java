package com.lifetracker.shared;

import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException e) {
    return problem(e.status, e.getMessage(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
    return problem(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields", fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class
  })
  ResponseEntity<ProblemDetail> malformed(Exception e) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", null);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ProblemDetail> size(Exception e) {
    return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be at most 5 MiB", null);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  ResponseEntity<ProblemDetail> conflict(Exception e) {
    return problem(
        HttpStatus.CONFLICT, "This record already exists or changed. Refresh and try again.", null);
  }

  private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message, Object fields) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, message);
    p.setProperty("correlationId", UUID.randomUUID().toString());
    if (fields != null) p.setProperty("errors", fields);
    return ResponseEntity.status(status).body(p);
  }
}
