package br.com.loja.repository;

import br.com.loja.model.BirthdayBenefit;
import org.springframework.data.jpa.repository.*;

public interface BirthdayBenefitRepository extends JpaRepository<BirthdayBenefit, Long> {}
