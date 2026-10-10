package br.com.florir.repository;
import br.com.florir.model.Customer;import org.springframework.data.jpa.repository.*;
public interface CustomerRepository extends JpaRepository<Customer,Long> { boolean existsByPhone(String phone);
}
