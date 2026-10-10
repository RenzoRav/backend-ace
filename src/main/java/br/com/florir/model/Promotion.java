package br.com.florir.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
public class Promotion extends BaseEntity {
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "percent", nullable = false, precision = 14, scale = 2)
  private BigDecimal percent;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;
}
