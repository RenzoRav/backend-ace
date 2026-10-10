package br.com.florir.service;

import br.com.florir.model.AuditLog;
import br.com.florir.repository.AuditLogRepository;
import br.com.florir.security.ActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
  private final AuditLogRepository auditLogRepository;
  private final ActorService actorService;

  public void record(String action, String entity, Long id) {
    if (!"MANAGER".equals(actorService.role())) return;
    var log = new AuditLog();
    log.setManager(actorService.manager());
    log.setAction(action);
    log.setEntity(entity);
    log.setEntityId(id.toString());
    auditLogRepository.save(log);
  }
}
