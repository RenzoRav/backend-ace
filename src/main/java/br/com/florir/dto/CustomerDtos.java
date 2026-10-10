package br.com.florir.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class CustomerDtos {
  public record CustomerInput(
      @NotBlank @Size(max = 255) String name, @NotBlank String phone, LocalDate birthday) {}

  public record CustomerView(String id, String name, String phone, LocalDate birthday) {}

  public record AddressInput(
      @NotBlank String label,
      @NotBlank @Pattern(regexp = "[0-9]{8}") String postalCode,
      @NotBlank String street,
      @NotBlank String number,
      String complement,
      @NotBlank String district,
      @NotBlank String city,
      @NotBlank @Pattern(regexp = "[A-Z]{2}") String state) {}

  public record AddressView(
      String id,
      String label,
      String postalCode,
      String street,
      String number,
      String complement,
      String district,
      String city,
      String state) {}
}
