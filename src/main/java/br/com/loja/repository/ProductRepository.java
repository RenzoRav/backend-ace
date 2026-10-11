package br.com.loja.repository;

import br.com.loja.model.Product;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  @Query("select coalesce(sum(p.cost * p.stock),0) from Product p")
  BigDecimal stockCost();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Product e where e.id = :id")
  Optional<Product> lockById(@Param("id") Long id);

  Page<Product> findByActiveTrue(Pageable pageable);

  @Query("select p from Product p where p.active = true and p.stock <= p.minimumStock")
  Page<Product> lowStock(Pageable page);

  @Query("select count(p) from Product p where p.active = true and p.stock <= p.minimumStock")
  long lowStockCount();
}
