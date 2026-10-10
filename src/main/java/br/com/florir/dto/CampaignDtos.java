package br.com.florir.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class CampaignDtos {
  public record CouponInput(
      @NotBlank @Size(max = 255) String code,
      @NotBlank @Pattern(regexp = "FIXED|PERCENT") String type,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal value,
      @NotNull LocalDate validFrom,
      @NotNull LocalDate validUntil,
      @Min(1) Integer usageLimit) {}

  public record CouponView(
      String id,
      String code,
      String type,
      BigDecimal value,
      LocalDate validFrom,
      LocalDate validUntil,
      Integer usageLimit,
      long usageCount) {}

  public record PromotionInput(
      @NotBlank @Size(max = 255) String name,
      @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
          BigDecimal percent,
      @NotNull LocalDate validFrom,
      @NotNull LocalDate validUntil,
      @NotEmpty @Size(max = 100) List<@NotBlank String> productIds) {}

  public record PromotionView(
      String id,
      String name,
      BigDecimal percent,
      LocalDate validFrom,
      LocalDate validUntil,
      List<String> productIds) {}

  public record RateInput(
      @Min(1) @Max(12) int installmentCount,
      @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 2) BigDecimal percent) {}

  public record RateView(String id, int installmentCount, BigDecimal percent) {}
}
