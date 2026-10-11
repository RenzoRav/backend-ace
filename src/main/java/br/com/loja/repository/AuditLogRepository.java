package br.com.loja.repository;

import br.com.loja.model.AuditLog;
import org.springframework.data.jpa.repository.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
