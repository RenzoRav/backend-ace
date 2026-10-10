package br.com.florir.repository;

import br.com.florir.model.ProductPromotion;
import org.springframework.data.jpa.repository.*;

public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {
  java.util.List<ProductPromotion> findByPromotionId(Long id);

  java.util.List<ProductPromotion> findByProductId(Long id);
}
