package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="gift")
public class Gift extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false)
 private Customer customer;
 public Customer getCustomer(){return customer;}
 public void setCustomer(Customer value){this.customer=value;}
 @Column(name="description",nullable=false)
 private String description;
 public String getDescription(){return description;}
 public void setDescription(String value){this.description=value;}
 @Column(name="date",nullable=false)
 private LocalDate date;
 public LocalDate getDate(){return date;}
 public void setDate(LocalDate value){this.date=value;}
}
