package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "financial_movement")
@Getter
@Setter
@NoArgsConstructor
public class FinancialMovement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "installment_id")
  private Installment installment;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;
}
