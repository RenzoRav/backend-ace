package br.com.loja.repository;

import br.com.loja.model.CustomerAddress;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
  List<CustomerAddress> findByCustomerId(Long id);

  Page<CustomerAddress> findByCustomerId(Long id, Pageable pageable);
}
