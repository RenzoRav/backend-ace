package br.com.loja.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private Brand brand;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", length = 4000)
  private String description;

  @Column(name = "image_url", length = 4000)
  private String imageUrl;

  @Column(name = "price", nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(name = "cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal cost;

  @Column(name = "stock", nullable = false)
  private int stock;

  @Column(name = "minimum_stock", nullable = false)
  private int minimumStock;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
