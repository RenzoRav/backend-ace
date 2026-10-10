package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupon")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends BaseEntity {
  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "value", nullable = false, precision = 14, scale = 2)
  private BigDecimal value;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;

  @Column(name = "usage_limit")
  private Integer usageLimit;
}
