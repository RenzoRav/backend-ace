package br.com.loja.repository;

import br.com.loja.model.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderIdOrderById(Long id);
}
