package br.com.florir.repository;
import br.com.florir.model.CustomerAccount;import org.springframework.data.jpa.repository.*;
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount,Long> { java.util.Optional<CustomerAccount> findByEmail(String email); boolean existsByEmail(String email);
}
