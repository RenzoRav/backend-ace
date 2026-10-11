package br.com.loja.repository;

import br.com.loja.model.StatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
  List<StatusHistory> findByOrderIdOrderByCreatedAt(Long id);
}
