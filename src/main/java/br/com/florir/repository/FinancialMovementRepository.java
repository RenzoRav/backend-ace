package br.com.florir.repository;
import br.com.florir.model.FinancialMovement;import org.springframework.data.jpa.repository.*;
public interface FinancialMovementRepository extends JpaRepository<FinancialMovement,Long> {
 java.util.List<FinancialMovement> findByOrderId(Long id); boolean existsByInstallmentIdAndType(Long id,String type);
}
