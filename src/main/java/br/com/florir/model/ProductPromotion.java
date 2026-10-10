package br.com.florir.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product_promotion")
@IdClass(ProductPromotionId.class)
public class ProductPromotion {
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  @Version private long version;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product value) {
    product = value;
  }

  public Promotion getPromotion() {
    return promotion;
  }

  public void setPromotion(Promotion value) {
    promotion = value;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
