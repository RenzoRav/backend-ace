package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="status_history")
public class StatusHistory extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false)
 private PurchaseOrder order;
 public PurchaseOrder getOrder(){return order;}
 public void setOrder(PurchaseOrder value){this.order=value;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="manager_id")
 private Manager manager;
 public Manager getManager(){return manager;}
 public void setManager(Manager value){this.manager=value;}
 @Column(name="previous_status")
 private String previousStatus;
 public String getPreviousStatus(){return previousStatus;}
 public void setPreviousStatus(String value){this.previousStatus=value;}
 @Column(name="next_status",nullable=false)
 private String nextStatus;
 public String getNextStatus(){return nextStatus;}
 public void setNextStatus(String value){this.nextStatus=value;}
}
