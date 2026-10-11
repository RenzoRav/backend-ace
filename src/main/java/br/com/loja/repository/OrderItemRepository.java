package br.com.loja.repository;

import br.com.loja.model.OrderItem;
import org.springframework.data.jpa.repository.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  java.util.List<OrderItem> findByOrderIdOrderById(Long id);
}
