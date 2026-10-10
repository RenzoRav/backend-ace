package br.com.florir.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_promotion")
@IdClass(ProductPromotionId.class)
@Getter
@NoArgsConstructor
public class ProductPromotion {
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  @Setter
  private Product product;

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_id", nullable = false)
  @Setter
  private Promotion promotion;

  @Version private long version;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();
}
