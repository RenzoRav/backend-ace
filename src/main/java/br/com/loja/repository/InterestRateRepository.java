package br.com.loja.repository;

import br.com.loja.model.InterestRate;
import org.springframework.data.jpa.repository.*;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {
  java.util.Optional<InterestRate> findByInstallmentCount(int count);
}
