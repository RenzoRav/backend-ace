package br.com.loja.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_rate_id")
  private InterestRate interestRate;

  @Column(name = "status", nullable = false)
  private String status = "NEW";

  @Column(name = "payment_status", nullable = false)
  private String paymentStatus = "PENDING";

  @Column(name = "payment_method", nullable = false)
  private String paymentMethod;

  @Column(name = "stock_effect", nullable = false)
  private String stockEffect = "NONE";

  @Column(name = "origin", nullable = false)
  private String origin;

  @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "discount", nullable = false, precision = 14, scale = 2)
  private BigDecimal discount;

  @Column(name = "shipping", nullable = false, precision = 14, scale = 2)
  private BigDecimal shipping;

  @Column(name = "interest", nullable = false, precision = 14, scale = 2)
  private BigDecimal interest;

  @Column(name = "total", nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Column(name = "installment_count", nullable = false)
  private int installmentCount = 1;

  @Column(name = "delivery", nullable = false)
  private boolean delivery;

  @Column(name = "address_snapshot", length = 4000)
  private String addressSnapshot;

  @Column(name = "idempotency_key", unique = true)
  private String idempotencyKey;

  @Column(name = "request_hash")
  private String requestHash;

  @Column(name = "customer_name", nullable = false)
  private String customerName;

  @Column(name = "customer_phone", nullable = false)
  private String customerPhone;
}
