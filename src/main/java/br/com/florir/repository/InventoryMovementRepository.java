package br.com.florir.repository;
import br.com.florir.model.InventoryMovement;import org.springframework.data.jpa.repository.*;
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement,Long> {
}
