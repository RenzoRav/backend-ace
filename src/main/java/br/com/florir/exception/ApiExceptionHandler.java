package br.com.florir.exception;

import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record Error(String message) {}

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Error> api(ApiException e) {
    return ResponseEntity.status(e.status()).body(new Error(e.getMessage()));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<Error> invalid(Exception e) {
    return ResponseEntity.badRequest().body(new Error("Dados inválidos"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Error> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            new Error(
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining("; "))));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    org.springframework.dao.PessimisticLockingFailureException.class
  })
  ResponseEntity<Error> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(new Error("Conflito de dados; atualize e tente novamente"));
  }
}
