package br.com.loja.repository;

import br.com.loja.model.RefreshSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from RefreshSession e where e.id = :id")
  Optional<RefreshSession> lockById(@Param("id") Long id);

  Optional<RefreshSession> findByTokenHash(String hash);

  List<RefreshSession> findByRoleAndAccountId(String role, Long id);
}
