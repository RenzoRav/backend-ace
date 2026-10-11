package br.com.loja.repository;

import br.com.loja.model.ProductPromotion;
import org.springframework.data.jpa.repository.*;

public interface ProductPromotionRepository
    extends JpaRepository<ProductPromotion, br.com.loja.model.ProductPromotionId> {
  java.util.List<ProductPromotion> findByPromotionId(Long id);

  java.util.List<ProductPromotion> findByProductId(Long id);
}
