package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(
    name = "birthday_benefit",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "year"}))
public class BirthdayBenefit extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer value) {
    this.customer = value;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  public Coupon getCoupon() {
    return coupon;
  }

  public void setCoupon(Coupon value) {
    this.coupon = value;
  }

  @Column(name = "year", nullable = false)
  private int year;

  public int getYear() {
    return year;
  }

  public void setYear(int value) {
    this.year = value;
  }

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(Instant value) {
    this.grantedAt = value;
  }
}
