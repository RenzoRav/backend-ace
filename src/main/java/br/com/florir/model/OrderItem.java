package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="order_item")
public class OrderItem extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false)
 private PurchaseOrder order;
 public PurchaseOrder getOrder(){return order;}
 public void setOrder(PurchaseOrder value){this.order=value;}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id",nullable=false)
 private Product product;
 public Product getProduct(){return product;}
 public void setProduct(Product value){this.product=value;}
 @Column(name="quantity",nullable=false)
 private int quantity;
 public int getQuantity(){return quantity;}
 public void setQuantity(int value){this.quantity=value;}
 @Column(name="unit_price",nullable=false,precision=14,scale=2)
 private BigDecimal unitPrice;
 public BigDecimal getUnitPrice(){return unitPrice;}
 public void setUnitPrice(BigDecimal value){this.unitPrice=value;}
 @Column(name="unit_cost",nullable=false,precision=14,scale=2)
 private BigDecimal unitCost;
 public BigDecimal getUnitCost(){return unitCost;}
 public void setUnitCost(BigDecimal value){this.unitCost=value;}
 @Column(name="product_name",nullable=false)
 private String productName;
 public String getProductName(){return productName;}
 public void setProductName(String value){this.productName=value;}
 @Column(name="brand_name",nullable=false)
 private String brandName;
 public String getBrandName(){return brandName;}
 public void setBrandName(String value){this.brandName=value;}
 @Column(name="category_name",nullable=false)
 private String categoryName;
 public String getCategoryName(){return categoryName;}
 public void setCategoryName(String value){this.categoryName=value;}
}
