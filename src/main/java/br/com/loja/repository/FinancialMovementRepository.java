package br.com.loja.repository;

import br.com.loja.model.FinancialMovement;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, Long> {
  @Query("select coalesce(sum(m.amount),0) from FinancialMovement m where m.type = :type")
  BigDecimal sumAmount(@Param("type") String type);

  List<FinancialMovement> findByOrderId(Long id);

  boolean existsByInstallmentIdAndType(Long id, String type);
}
