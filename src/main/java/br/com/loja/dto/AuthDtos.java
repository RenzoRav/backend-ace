package br.com.loja.dto;

import br.com.loja.validation.Utf8ByteSize;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {
  public record Login(
      @NotBlank @Email @Size(max = 255) String email, @NotBlank @Utf8ByteSize String password) {}

  public record Register(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(max = 32) String phone,
      @NotBlank @Size(min = 10, max = 72) @Utf8ByteSize String password,
      @PastOrPresent LocalDate birthday) {}

  public record Session(
      String accessToken,
      String tokenType,
      long expiresIn,
      String role,
      String accountId,
      String name) {}

  public record PasswordChange(
      @NotBlank @Utf8ByteSize String currentPassword,
      @NotBlank @Size(min = 10, max = 72) @Utf8ByteSize String newPassword) {}
}
