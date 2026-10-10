package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;
public final class FinanceDtos {
 public record EntryInput(@NotBlank @Pattern(regexp="IN|OUT") String type,@NotBlank @Size(max=255) String description,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount){}
 public record EntryView(String id,String orderId,String installmentId,String type,String description,BigDecimal amount,Instant date){}
 public record StockView(String id,String productId,String orderId,String type,int quantity,String description,Instant date){}
 public record AuditView(String id,String managerId,String managerName,String action,String entity,String entityId,Instant date){}
 public record Summary(long customers,long products,long orders,BigDecimal received,BigDecimal outgoing,BigDecimal balance,BigDecimal stockCost){}
}
