package br.com.loja.repository;

import br.com.loja.model.ProductPromotion;
import br.com.loja.model.ProductPromotionId;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface ProductPromotionRepository
    extends JpaRepository<ProductPromotion, ProductPromotionId> {
  List<ProductPromotion> findByPromotionId(Long id);

  List<ProductPromotion> findByProductId(Long id);
}
