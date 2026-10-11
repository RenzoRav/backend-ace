package br.com.loja.repository;

import br.com.loja.model.StatusHistory;
import org.springframework.data.jpa.repository.*;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
  java.util.List<StatusHistory> findByOrderIdOrderByCreatedAt(Long id);
}
