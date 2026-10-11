package br.com.loja.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gift")
@Getter
@Setter
@NoArgsConstructor
public class Gift extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "date", nullable = false)
  private LocalDate date;
}
