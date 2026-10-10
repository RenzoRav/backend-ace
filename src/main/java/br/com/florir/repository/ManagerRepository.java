package br.com.florir.repository;

import br.com.florir.model.Manager;
import org.springframework.data.jpa.repository.*;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<Manager> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Manager e where e.id = :id")
  java.util.Optional<Manager> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);
}
