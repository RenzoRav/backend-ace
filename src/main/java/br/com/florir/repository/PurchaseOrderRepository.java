package br.com.florir.repository;

import br.com.florir.model.PurchaseOrder;
import org.springframework.data.jpa.repository.*;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from PurchaseOrder e where e.id = :id")
  java.util.Optional<PurchaseOrder> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);

  java.util.List<PurchaseOrder> findByCustomerIdOrderByCreatedAtDesc(Long id);

  java.util.Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);

  long countByCouponIdAndStatusNot(Long id, String status);
}
