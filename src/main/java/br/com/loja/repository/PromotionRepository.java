package br.com.loja.repository;

import br.com.loja.model.Promotion;
import org.springframework.data.jpa.repository.*;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {}
