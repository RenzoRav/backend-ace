package br.com.loja.repository;

import br.com.loja.model.SessionFamily;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionFamilyRepository extends JpaRepository<SessionFamily, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select family from SessionFamily family where family.id=:id")
  Optional<SessionFamily> lockById(@Param("id") String id);

  List<SessionFamily> findByRoleAndAccountId(String role, Long accountId);
}
