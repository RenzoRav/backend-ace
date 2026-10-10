package br.com.florir.dto;

import java.math.BigDecimal;
import java.time.*;

public final class DashboardDtos {
  public record Alerts(long lowStock, long overdueInstallments) {}

  public record InstallmentView(
      String id,
      String orderId,
      String customerId,
      String customerName,
      String customerPhone,
      int number,
      BigDecimal amount,
      LocalDate dueDate,
      String status,
      Instant paidAt) {}
}
