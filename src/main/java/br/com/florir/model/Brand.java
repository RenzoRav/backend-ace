package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="brand")
public class Brand extends BaseEntity {
 @Column(name="name",nullable=false,unique=true)
 private String name;
 public String getName(){return name;}
 public void setName(String value){this.name=value;}
}
