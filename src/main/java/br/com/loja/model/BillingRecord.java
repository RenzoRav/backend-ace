package br.com.loja.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "billing_record")
@Getter
@Setter
@NoArgsConstructor
public class BillingRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "installment_id", nullable = false)
  private Installment installment;

  @Column(name = "message", nullable = false, length = 4000)
  private String message;

  @Column(name = "declared_sent_at", nullable = false)
  private Instant declaredSentAt;
}
