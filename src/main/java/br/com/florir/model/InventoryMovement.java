package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product value) {
    this.product = value;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  public PurchaseOrder getOrder() {
    return order;
  }

  public void setOrder(PurchaseOrder value) {
    this.order = value;
  }

  @Column(name = "type", nullable = false)
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String value) {
    this.type = value;
  }

  @Column(name = "quantity", nullable = false)
  private int quantity;

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int value) {
    this.quantity = value;
  }

  @Column(name = "description", length = 4000)
  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }
}
