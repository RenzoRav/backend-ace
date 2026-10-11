package br.com.loja.repository;

import br.com.loja.model.PurchaseOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from PurchaseOrder e where e.id = :id")
  Optional<PurchaseOrder> lockById(@Param("id") Long id);

  List<PurchaseOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);

  Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);

  long countByCouponIdAndStatusNot(Long id, String status);
}
