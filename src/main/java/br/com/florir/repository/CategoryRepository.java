package br.com.florir.repository;
import br.com.florir.model.Category;import org.springframework.data.jpa.repository.*;
public interface CategoryRepository extends JpaRepository<Category,Long> { boolean existsByNameIgnoreCase(String name);
}
