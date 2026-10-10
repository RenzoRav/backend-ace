package br.com.florir.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {
  @Column(name = "name", nullable = false)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  @Column(name = "phone", nullable = false, unique = true)
  private String phone;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    this.phone = value;
  }

  @Column(name = "birthday")
  private LocalDate birthday;

  public LocalDate getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDate value) {
    this.birthday = value;
  }
}
