package br.com.florir.repository;

import br.com.florir.model.Installment;
import org.springframework.data.jpa.repository.*;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Installment e where e.id = :id")
  java.util.Optional<Installment> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);

  java.util.List<Installment> findByOrderIdOrderByNumber(Long id);

  org.springframework.data.domain.Page<Installment> findByStatus(
      String status, org.springframework.data.domain.Pageable page);

  org.springframework.data.domain.Page<Installment> findByStatusAndDueDateBefore(
      String status, java.time.LocalDate date, org.springframework.data.domain.Pageable page);

  long countByStatusAndDueDateBefore(String status, java.time.LocalDate date);
}
