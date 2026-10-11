package br.com.loja.repository;

import br.com.loja.model.CustomerAddress;
import org.springframework.data.jpa.repository.*;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
  java.util.List<CustomerAddress> findByCustomerId(Long id);
}
