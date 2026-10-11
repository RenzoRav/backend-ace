package br.com.loja.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {
  public record Login(@NotBlank @Email String email, @NotBlank String password) {}

  public record Register(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Email String email,
      @NotBlank String phone,
      @NotBlank @Size(min = 10, max = 72) String password,
      LocalDate birthday) {}

  public record Session(
      String accessToken,
      String tokenType,
      long expiresIn,
      String role,
      String accountId,
      String name) {}

  public record PasswordChange(
      @NotBlank String currentPassword, @NotBlank @Size(min = 10, max = 72) String newPassword) {}
}
