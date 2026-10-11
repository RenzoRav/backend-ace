package br.com.loja.repository;

import br.com.loja.model.Installment;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Installment e where e.id = :id")
  Optional<Installment> lockById(@Param("id") Long id);

  List<Installment> findByOrderIdOrderByNumber(Long id);

  Page<Installment> findByStatus(String status, Pageable page);

  Page<Installment> findByStatusAndDueDateBefore(String status, LocalDate date, Pageable page);

  long countByStatusAndDueDateBefore(String status, LocalDate date);
}
