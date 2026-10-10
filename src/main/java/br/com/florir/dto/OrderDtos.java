package br.com.florir.dto;
import jakarta.validation.*;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.*;import java.util.List;
public final class OrderDtos {
 public record ItemInput(@NotBlank String productId,@Min(1) @Max(10000) int quantity){}
 public record OrderInput(String customerId,@NotBlank @Pattern(regexp="PIX|CASH|CARD|BOLETO|INSTALLMENTS") String paymentMethod,@Min(1) @Max(12) int installmentCount,boolean delivery,String addressId,String couponCode,@NotEmpty @Size(max=100) List<@Valid ItemInput> items){}
 public record ItemView(String id,String productId,String productName,String brandName,String categoryName,int quantity,BigDecimal unitPrice){}
 public record InstallmentView(String id,int number,BigDecimal amount,LocalDate dueDate,String status,Instant paidAt){}
 public record HistoryView(String previousStatus,String nextStatus,Instant date){}
 public record OrderView(String id,String customerId,String customerName,String customerPhone,String status,String paymentStatus,String paymentMethod,String stockEffect,BigDecimal subtotal,BigDecimal discount,BigDecimal shipping,BigDecimal interest,BigDecimal total,int installmentCount,boolean delivery,String addressSnapshot,Instant createdAt,List<ItemView> items,List<InstallmentView> installments,List<HistoryView> history){}
 public record StatusInput(@NotBlank @Pattern(regexp="PREPARING|FINISHED") String status){}
}
