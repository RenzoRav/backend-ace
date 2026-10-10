package br.com.florir.repository;

import br.com.florir.model.InterestRate;
import org.springframework.data.jpa.repository.*;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {
  java.util.Optional<InterestRate> findByInstallmentCount(int count);
}
