package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(
    name = "installment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "number"}))
public class Installment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private PurchaseOrder order;

  public PurchaseOrder getOrder() {
    return order;
  }

  public void setOrder(PurchaseOrder value) {
    this.order = value;
  }

  @Column(name = "number", nullable = false)
  private int number;

  public int getNumber() {
    return number;
  }

  public void setNumber(int value) {
    this.number = value;
  }

  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal value) {
    this.amount = value;
  }

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate value) {
    this.dueDate = value;
  }

  @Column(name = "status", nullable = false)
  private String status = "PENDING";

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  @Column(name = "paid_at")
  private Instant paidAt;

  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant value) {
    this.paidAt = value;
  }
}
