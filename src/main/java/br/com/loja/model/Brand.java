package br.com.loja.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brand")
@Getter
@Setter
@NoArgsConstructor
public class Brand extends BaseEntity {
  @Column(name = "name", nullable = false, unique = true)
  private String name;
}
