package br.com.florir.repository;
import br.com.florir.model.OrderItem;import org.springframework.data.jpa.repository.*;
public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
 java.util.List<OrderItem> findByOrderIdOrderById(Long id);
}
