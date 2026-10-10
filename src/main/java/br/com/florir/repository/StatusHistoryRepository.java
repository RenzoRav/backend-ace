package br.com.florir.repository;

import br.com.florir.model.StatusHistory;
import org.springframework.data.jpa.repository.*;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
  java.util.List<StatusHistory> findByOrderIdOrderByCreatedAt(Long id);
}
