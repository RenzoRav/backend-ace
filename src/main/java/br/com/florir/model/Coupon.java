package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "coupon")
public class Coupon extends BaseEntity {
  @Column(name = "code", nullable = false, unique = true)
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    this.code = value;
  }

  @Column(name = "type", nullable = false)
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String value) {
    this.type = value;
  }

  @Column(name = "value", nullable = false, precision = 14, scale = 2)
  private BigDecimal value;

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  public LocalDate getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(LocalDate value) {
    this.validFrom = value;
  }

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDate value) {
    this.validUntil = value;
  }

  @Column(name = "usage_limit")
  private Integer usageLimit;

  public Integer getUsageLimit() {
    return usageLimit;
  }

  public void setUsageLimit(Integer value) {
    this.usageLimit = value;
  }
}
