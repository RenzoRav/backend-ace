package br.com.florir.repository;
import br.com.florir.model.SupportTicket;import org.springframework.data.jpa.repository.*;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long> {
 java.util.List<SupportTicket> findByCustomerId(Long id);
}
