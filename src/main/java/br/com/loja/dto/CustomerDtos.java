package br.com.loja.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class CustomerDtos {
  public record CustomerInput(
      @NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 32) String phone, @PastOrPresent LocalDate birthday) {}

  public record CustomerView(String id, String name, String phone, @PastOrPresent LocalDate birthday) {}

  public record AddressInput(
      @NotBlank @Size(max = 255) String label,
      @NotBlank @Pattern(regexp = "[0-9]{8}") String postalCode,
      @NotBlank @Size(max = 255) String street,
      @NotBlank @Size(max = 255) String number,
      @Size(max = 255) String complement,
      @NotBlank @Size(max = 255) String district,
      @NotBlank @Size(max = 255) String city,
      @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO") String state) {}

  public record AddressView(
      String id,
      String label,
      String postalCode,
      String street,
      String number,
      @Size(max = 255) String complement,
      String district,
      String city,
      String state) {}
}

