package br.com.loja.repository;

import br.com.loja.model.CustomerAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CustomerAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CustomerAccount e where e.id = :id")
  Optional<CustomerAccount> lockById(@Param("id") Long id);
}
