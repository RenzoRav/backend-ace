package br.com.florir.repository;
import br.com.florir.model.Manager;import org.springframework.data.jpa.repository.*;
public interface ManagerRepository extends JpaRepository<Manager,Long> { java.util.Optional<Manager> findByEmail(String email); boolean existsByEmail(String email);
}
