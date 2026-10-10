package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="purchase_order")
public class PurchaseOrder extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false)
 private Customer customer;
 public Customer getCustomer(){return customer;}
 public void setCustomer(Customer value){this.customer=value;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="coupon_id")
 private Coupon coupon;
 public Coupon getCoupon(){return coupon;}
 public void setCoupon(Coupon value){this.coupon=value;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="interest_rate_id")
 private InterestRate interestRate;
 public InterestRate getInterestRate(){return interestRate;}
 public void setInterestRate(InterestRate value){this.interestRate=value;}
 @Column(name="status",nullable=false)
 private String status="NEW";
 public String getStatus(){return status;}
 public void setStatus(String value){this.status=value;}
 @Column(name="payment_status",nullable=false)
 private String paymentStatus="PENDING";
 public String getPaymentStatus(){return paymentStatus;}
 public void setPaymentStatus(String value){this.paymentStatus=value;}
 @Column(name="payment_method",nullable=false)
 private String paymentMethod;
 public String getPaymentMethod(){return paymentMethod;}
 public void setPaymentMethod(String value){this.paymentMethod=value;}
 @Column(name="stock_effect",nullable=false)
 private String stockEffect="NONE";
 public String getStockEffect(){return stockEffect;}
 public void setStockEffect(String value){this.stockEffect=value;}
 @Column(name="origin",nullable=false)
 private String origin;
 public String getOrigin(){return origin;}
 public void setOrigin(String value){this.origin=value;}
 @Column(name="subtotal",nullable=false,precision=14,scale=2)
 private BigDecimal subtotal;
 public BigDecimal getSubtotal(){return subtotal;}
 public void setSubtotal(BigDecimal value){this.subtotal=value;}
 @Column(name="discount",nullable=false,precision=14,scale=2)
 private BigDecimal discount;
 public BigDecimal getDiscount(){return discount;}
 public void setDiscount(BigDecimal value){this.discount=value;}
 @Column(name="shipping",nullable=false,precision=14,scale=2)
 private BigDecimal shipping;
 public BigDecimal getShipping(){return shipping;}
 public void setShipping(BigDecimal value){this.shipping=value;}
 @Column(name="interest",nullable=false,precision=14,scale=2)
 private BigDecimal interest;
 public BigDecimal getInterest(){return interest;}
 public void setInterest(BigDecimal value){this.interest=value;}
 @Column(name="total",nullable=false,precision=14,scale=2)
 private BigDecimal total;
 public BigDecimal getTotal(){return total;}
 public void setTotal(BigDecimal value){this.total=value;}
 @Column(name="installment_count",nullable=false)
 private int installmentCount=1;
 public int getInstallmentCount(){return installmentCount;}
 public void setInstallmentCount(int value){this.installmentCount=value;}
 @Column(name="delivery",nullable=false)
 private boolean delivery;
 public boolean getDelivery(){return delivery;}
 public void setDelivery(boolean value){this.delivery=value;}
 @Column(name="address_snapshot",length=4000)
 private String addressSnapshot;
 public String getAddressSnapshot(){return addressSnapshot;}
 public void setAddressSnapshot(String value){this.addressSnapshot=value;}
 @Column(name="idempotency_key",unique=true)
 private String idempotencyKey;
 public String getIdempotencyKey(){return idempotencyKey;}
 public void setIdempotencyKey(String value){this.idempotencyKey=value;}
 @Column(name="request_hash")
 private String requestHash;
 public String getRequestHash(){return requestHash;}
 public void setRequestHash(String value){this.requestHash=value;}
 @Column(name="customer_name",nullable=false)
 private String customerName;
 public String getCustomerName(){return customerName;}
 public void setCustomerName(String value){this.customerName=value;}
 @Column(name="customer_phone",nullable=false)
 private String customerPhone;
 public String getCustomerPhone(){return customerPhone;}
 public void setCustomerPhone(String value){this.customerPhone=value;}
}
