package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interest_rate")
@Getter
@Setter
@NoArgsConstructor
public class InterestRate extends BaseEntity {
  @Column(name = "installment_count", nullable = false, unique = true)
  private int installmentCount;

  @Column(name = "percent", nullable = false, precision = 14, scale = 2)
  private BigDecimal percent;
}
