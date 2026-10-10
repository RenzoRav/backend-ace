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
  ResponseEntity<Error> api(ApiException exception) {
    return ResponseEntity.status(exception.status()).body(new Error(exception.getMessage()));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<Error> invalid(Exception exception) {
    return ResponseEntity.badRequest().body(new Error("Dados inválidos"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Error> validation(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest()
        .body(
            new Error(
                exception.getBindingResult().getFieldErrors().stream()
                    .map(
                        fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining("; "))));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    org.springframework.dao.PessimisticLockingFailureException.class
  })
  ResponseEntity<Error> conflict(Exception exception) {
    return ResponseEntity.status(409)
        .body(new Error("Conflito de dados; atualize e tente novamente"));
  }
}
