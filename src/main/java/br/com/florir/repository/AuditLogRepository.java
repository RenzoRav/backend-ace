package br.com.florir.repository;

import br.com.florir.model.AuditLog;
import org.springframework.data.jpa.repository.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
