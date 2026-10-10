package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "billing_record")
public class BillingRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private Installment installment;

  public Installment getInstallment() {
    return installment;
  }

  public void setInstallment(Installment value) {
    this.installment = value;
  }

  @Column(name = "message", nullable = false, length = 4000)
  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String value) {
    this.message = value;
  }

  @Column(name = "declared_sent_at", nullable = false)
  private Instant declaredSentAt;

  public Instant getDeclaredSentAt() {
    return declaredSentAt;
  }

  public void setDeclaredSentAt(Instant value) {
    this.declaredSentAt = value;
  }
}
