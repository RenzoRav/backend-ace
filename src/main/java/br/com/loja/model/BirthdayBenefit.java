package br.com.loja.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "birthday_benefit",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
public class BirthdayBenefit extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  @Column(name = "year", nullable = false)
  private int year;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;
}
