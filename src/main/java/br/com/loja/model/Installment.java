package br.com.loja.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "installment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "number"}))
@Getter
@Setter
@NoArgsConstructor
public class Installment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private PurchaseOrder order;

  @Column(name = "number", nullable = false)
  private int number;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "status", nullable = false)
  private String status = "PENDING";

  @Column(name = "paid_at")
  private Instant paidAt;
}
