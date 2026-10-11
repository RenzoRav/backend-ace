package br.com.loja.dto;

import jakarta.validation.constraints.*;
import br.com.loja.validation.PositiveIdentifier;
import java.time.*;

public final class RelationshipDtos {
  public record BenefitInput(
      @NotBlank @PositiveIdentifier String customerId, @PositiveIdentifier String couponId, @Min(1900) @Max(3000) int year) {}

  public record BenefitView(
      String id,
      @PositiveIdentifier String customerId,
      String customerName,
      @PositiveIdentifier String couponId,
      int year,
      Instant grantedAt) {}

  public record GiftInput(
      @NotBlank @PositiveIdentifier String customerId,
      @NotBlank @Size(max = 255) String description,
      @NotNull LocalDate date) {}

  public record GiftView(
      String id, @PositiveIdentifier String customerId, String customerName, String description, LocalDate date) {}

  public record BillingInput(@NotBlank @Size(max = 4000) String message) {}

  public record BillingView(
      String id, String installmentId, String message, Instant declaredSentAt) {}

  public record WhatsAppPreview(String installmentId, String message, String url, boolean sent) {}

  public record TicketInput(
      @PositiveIdentifier String orderId,
      @NotBlank @Size(max = 255) String subject,
      @NotBlank @Size(max = 4000) String message) {}

  public record ReplyInput(
      @NotBlank @Size(max = 4000) String reply,
      @NotBlank @Pattern(regexp = "OPEN|RESOLVED") String status) {}

  public record TicketView(
      String id,
      @PositiveIdentifier String customerId,
      @PositiveIdentifier String orderId,
      String subject,
      String message,
      String status,
      String reply,
      Instant createdAt) {}
}

