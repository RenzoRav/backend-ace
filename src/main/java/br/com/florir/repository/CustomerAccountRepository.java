package br.com.florir.repository;

import br.com.florir.model.CustomerAccount;
import org.springframework.data.jpa.repository.*;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  java.util.Optional<CustomerAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CustomerAccount e where e.id = :id")
  java.util.Optional<CustomerAccount> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);
}
