package br.com.florir.exception;

public class ApiException extends RuntimeException {
  private final int status;

  public ApiException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }

  public static ApiException missing() {
    return new ApiException(404, "Registro não encontrado");
  }

  public static ApiException conflict(String m) {
    return new ApiException(409, m);
  }
}
