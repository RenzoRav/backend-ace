package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "customer_address")
public class CustomerAddress extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer value) {
    this.customer = value;
  }

  @Column(name = "label", nullable = false)
  private String label;

  public String getLabel() {
    return label;
  }

  public void setLabel(String value) {
    this.label = value;
  }

  @Column(name = "postal_code", nullable = false)
  private String postalCode;

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String value) {
    this.postalCode = value;
  }

  @Column(name = "street", nullable = false)
  private String street;

  public String getStreet() {
    return street;
  }

  public void setStreet(String value) {
    this.street = value;
  }

  @Column(name = "number", nullable = false)
  private String number;

  public String getNumber() {
    return number;
  }

  public void setNumber(String value) {
    this.number = value;
  }

  @Column(name = "complement")
  private String complement;

  public String getComplement() {
    return complement;
  }

  public void setComplement(String value) {
    this.complement = value;
  }

  @Column(name = "district", nullable = false)
  private String district;

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String value) {
    this.district = value;
  }

  @Column(name = "city", nullable = false)
  private String city;

  public String getCity() {
    return city;
  }

  public void setCity(String value) {
    this.city = value;
  }

  @Column(name = "state", nullable = false)
  private String state;

  public String getState() {
    return state;
  }

  public void setState(String value) {
    this.state = value;
  }
}
