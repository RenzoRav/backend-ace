package br.com.loja.repository;

import br.com.loja.model.Brand;
import org.springframework.data.jpa.repository.*;

public interface BrandRepository extends JpaRepository<Brand, Long> {
  boolean existsByNameIgnoreCase(String name);
}
