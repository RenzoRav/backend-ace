package br.com.loja.repository;

import br.com.loja.model.FinancialMovement;
import org.springframework.data.jpa.repository.*;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, Long> {
  @Query("select coalesce(sum(m.amount),0) from FinancialMovement m where m.type = :type")
  java.math.BigDecimal sumAmount(
      @org.springframework.data.repository.query.Param("type") String type);

  java.util.List<FinancialMovement> findByOrderId(Long id);

  boolean existsByInstallmentIdAndType(Long id, String type);
}
