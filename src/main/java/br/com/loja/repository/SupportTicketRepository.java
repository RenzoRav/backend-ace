package br.com.loja.repository;

import br.com.loja.model.SupportTicket;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
  List<SupportTicket> findByCustomerId(Long id);

  Page<SupportTicket> findByCustomerId(Long id, Pageable pageable);
}
