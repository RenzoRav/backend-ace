package br.com.loja.exception;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.*;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record Error(String message) {}
  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Error> unavailable(DataAccessException exception){return ResponseEntity.status(503).body(new Error("Servico temporariamente indisponivel"));}

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Error> api(ApiException exception) {
    return ResponseEntity.status(exception.status()).body(new Error(exception.getMessage()));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
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
                    .collect(Collectors.joining("; "))));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<Error> conflict(Exception exception) {
    return ResponseEntity.status(409)
        .body(new Error("Conflito de dados; atualize e tente novamente"));
  }
}

