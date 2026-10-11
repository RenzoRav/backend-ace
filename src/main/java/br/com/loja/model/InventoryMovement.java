package br.com.loja.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_movement")
@Getter
@Setter
@NoArgsConstructor
public class InventoryMovement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "description", length = 4000)
  private String description;
}
