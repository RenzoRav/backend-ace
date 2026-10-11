package br.com.loja.repository;

import br.com.loja.model.InventoryMovement;
import org.springframework.data.jpa.repository.*;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {}
