package br.com.florir.model;

import java.io.Serializable;
import java.util.Objects;

public class ProductPromotionId implements Serializable {
  private Long product;
  private Long promotion;

  public ProductPromotionId() {}

  public ProductPromotionId(Long product, Long promotion) {
    this.product = product;
    this.promotion = promotion;
  }

  public Long getProduct() {
    return product;
  }

  public void setProduct(Long value) {
    product = value;
  }

  public Long getPromotion() {
    return promotion;
  }

  public void setPromotion(Long value) {
    promotion = value;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ProductPromotionId id
        && Objects.equals(product, id.product)
        && Objects.equals(promotion, id.promotion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(product, promotion);
  }
}
