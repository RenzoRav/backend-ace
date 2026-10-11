package br.com.loja.repository;

import br.com.loja.model.RefreshSession;
import org.springframework.data.jpa.repository.*;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from RefreshSession e where e.id = :id")
  java.util.Optional<RefreshSession> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);

  java.util.Optional<RefreshSession> findByTokenHash(String hash);

  java.util.List<RefreshSession> findByRoleAndAccountId(String role, Long id);
}
