package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "promotion")
public class Promotion extends BaseEntity {
  @Column(name = "name", nullable = false)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  @Column(name = "percent", nullable = false, precision = 14, scale = 2)
  private BigDecimal percent;

  public BigDecimal getPercent() {
    return percent;
  }

  public void setPercent(BigDecimal value) {
    this.percent = value;
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
}
