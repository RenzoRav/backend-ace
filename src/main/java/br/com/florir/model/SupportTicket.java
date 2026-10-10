package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="support_ticket")
public class SupportTicket extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false)
 private Customer customer;
 public Customer getCustomer(){return customer;}
 public void setCustomer(Customer value){this.customer=value;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id")
 private PurchaseOrder order;
 public PurchaseOrder getOrder(){return order;}
 public void setOrder(PurchaseOrder value){this.order=value;}
 @Column(name="subject",nullable=false)
 private String subject;
 public String getSubject(){return subject;}
 public void setSubject(String value){this.subject=value;}
 @Column(name="message",nullable=false,length=4000)
 private String message;
 public String getMessage(){return message;}
 public void setMessage(String value){this.message=value;}
 @Column(name="status",nullable=false)
 private String status="OPEN";
 public String getStatus(){return status;}
 public void setStatus(String value){this.status=value;}
 @Column(name="reply",length=4000)
 private String reply;
 public String getReply(){return reply;}
 public void setReply(String value){this.reply=value;}
}
