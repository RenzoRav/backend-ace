package br.com.loja.repository;

import br.com.loja.model.InterestRate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {
  Optional<InterestRate> findByInstallmentCount(int count);
}
