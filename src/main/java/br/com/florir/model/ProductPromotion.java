package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(
    name = "product_promotion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "promotion_id"}))
public class ProductPromotion extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product value) {
    this.product = value;
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  public Promotion getPromotion() {
    return promotion;
  }

  public void setPromotion(Promotion value) {
    this.promotion = value;
  }
}
