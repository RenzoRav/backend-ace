package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_ticket")
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  @Column(name = "subject", nullable = false)
  private String subject;

  @Column(name = "message", nullable = false, length = 4000)
  private String message;

  @Column(name = "status", nullable = false)
  private String status = "OPEN";

  @Column(name = "reply", length = 4000)
  private String reply;
}
