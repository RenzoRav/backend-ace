package br.com.loja.dto;

import jakarta.validation.constraints.*;
import java.time.*;

public final class RelationshipDtos {
  public record BenefitInput(
      @NotBlank String customerId, String couponId, @Min(1900) @Max(3000) int year) {}

  public record BenefitView(
      String id,
      String customerId,
      String customerName,
      String couponId,
      int year,
      Instant grantedAt) {}

  public record GiftInput(
      @NotBlank String customerId,
      @NotBlank @Size(max = 255) String description,
      @NotNull LocalDate date) {}

  public record GiftView(
      String id, String customerId, String customerName, String description, LocalDate date) {}

  public record BillingInput(@NotBlank @Size(max = 4000) String message) {}

  public record BillingView(
      String id, String installmentId, String message, Instant declaredSentAt) {}

  public record WhatsAppPreview(String installmentId, String message, String url, boolean sent) {}

  public record TicketInput(
      String orderId,
      @NotBlank @Size(max = 255) String subject,
      @NotBlank @Size(max = 4000) String message) {}

  public record ReplyInput(
      @NotBlank @Size(max = 4000) String reply,
      @NotBlank @Pattern(regexp = "OPEN|RESOLVED") String status) {}

  public record TicketView(
      String id,
      String customerId,
      String orderId,
      String subject,
      String message,
      String status,
      String reply,
      Instant createdAt) {}
}
