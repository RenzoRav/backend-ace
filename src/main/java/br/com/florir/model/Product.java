package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="product")
public class Product extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="brand_id",nullable=false)
 private Brand brand;
 public Brand getBrand(){return brand;}
 public void setBrand(Brand value){this.brand=value;}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id",nullable=false)
 private Category category;
 public Category getCategory(){return category;}
 public void setCategory(Category value){this.category=value;}
 @Column(name="name",nullable=false)
 private String name;
 public String getName(){return name;}
 public void setName(String value){this.name=value;}
 @Column(name="description",length=4000)
 private String description;
 public String getDescription(){return description;}
 public void setDescription(String value){this.description=value;}
 @Column(name="image_url",length=4000)
 private String imageUrl;
 public String getImageUrl(){return imageUrl;}
 public void setImageUrl(String value){this.imageUrl=value;}
 @Column(name="price",nullable=false,precision=14,scale=2)
 private BigDecimal price;
 public BigDecimal getPrice(){return price;}
 public void setPrice(BigDecimal value){this.price=value;}
 @Column(name="cost",nullable=false,precision=14,scale=2)
 private BigDecimal cost;
 public BigDecimal getCost(){return cost;}
 public void setCost(BigDecimal value){this.cost=value;}
 @Column(name="stock",nullable=false)
 private int stock;
 public int getStock(){return stock;}
 public void setStock(int value){this.stock=value;}
 @Column(name="minimum_stock",nullable=false)
 private int minimumStock;
 public int getMinimumStock(){return minimumStock;}
 public void setMinimumStock(int value){this.minimumStock=value;}
 @Column(name="active",nullable=false)
 private boolean active=true;
 public boolean getActive(){return active;}
 public void setActive(boolean value){this.active=value;}
}
