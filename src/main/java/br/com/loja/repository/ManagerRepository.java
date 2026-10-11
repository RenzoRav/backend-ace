package br.com.loja.repository;

import br.com.loja.model.Manager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Manager> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Manager e where e.id = :id")
  Optional<Manager> lockById(@Param("id") Long id);
}
