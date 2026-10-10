package br.com.florir.repository;

import br.com.florir.model.Product;
import org.springframework.data.jpa.repository.*;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @Query("select coalesce(sum(p.cost * p.stock),0) from Product p")
  java.math.BigDecimal stockCost();

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Product e where e.id = :id")
  java.util.Optional<Product> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);

  org.springframework.data.domain.Page<Product> findByActiveTrue(
      org.springframework.data.domain.Pageable pageable);
}
