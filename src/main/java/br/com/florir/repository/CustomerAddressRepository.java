package br.com.florir.repository;

import br.com.florir.model.CustomerAddress;
import org.springframework.data.jpa.repository.*;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
  java.util.List<CustomerAddress> findByCustomerId(Long id);
}
