package br.com.florir.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="interest_rate")
public class InterestRate extends BaseEntity {
 @Column(name="installment_count",nullable=false,unique=true)
 private int installmentCount;
 public int getInstallmentCount(){return installmentCount;}
 public void setInstallmentCount(int value){this.installmentCount=value;}
 @Column(name="percent",nullable=false,precision=14,scale=2)
 private BigDecimal percent;
 public BigDecimal getPercent(){return percent;}
 public void setPercent(BigDecimal value){this.percent=value;}
}
