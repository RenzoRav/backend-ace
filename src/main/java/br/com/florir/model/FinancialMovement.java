package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "financial_movement")
public class FinancialMovement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  public PurchaseOrder getOrder() {
    return order;
  }

  public void setOrder(PurchaseOrder value) {
    this.order = value;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "installment_id")
  private Installment installment;

  public Installment getInstallment() {
    return installment;
  }

  public void setInstallment(Installment value) {
    this.installment = value;
  }

  @Column(name = "type", nullable = false)
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String value) {
    this.type = value;
  }

  @Column(name = "description", nullable = false)
  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal value) {
    this.amount = value;
  }
}
