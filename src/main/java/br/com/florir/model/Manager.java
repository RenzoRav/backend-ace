package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="manager")
public class Manager extends BaseEntity {
 @Column(name="name",nullable=false)
 private String name;
 public String getName(){return name;}
 public void setName(String value){this.name=value;}
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
 @Column(name="active",nullable=false)
 private boolean active=true;
 public boolean getActive(){return active;}
 public void setActive(boolean value){this.active=value;}
}
