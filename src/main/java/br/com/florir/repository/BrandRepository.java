package br.com.florir.repository;

import br.com.florir.model.Brand;
import org.springframework.data.jpa.repository.*;

public interface BrandRepository extends JpaRepository<Brand, Long> {
  boolean existsByNameIgnoreCase(String name);
}
