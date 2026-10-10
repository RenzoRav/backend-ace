package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="customer_account")
public class CustomerAccount extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false,unique=true)
 private Customer customer;
 public Customer getCustomer(){return customer;}
 public void setCustomer(Customer value){this.customer=value;}
 @Column(name="email",nullable=false,unique=true)
 private String email;
 public String getEmail(){return email;}
 public void setEmail(String value){this.email=value;}
 @Column(name="password_hash",nullable=false)
 private String passwordHash;
 public String getPasswordHash(){return passwordHash;}
 public void setPasswordHash(String value){this.passwordHash=value;}
 @Column(name="credential_version",nullable=false)
 private long credentialVersion;
 public long getCredentialVersion(){return credentialVersion;}
 public void setCredentialVersion(long value){this.credentialVersion=value;}
}
