package br.com.florir.service;
import br.com.florir.repository.AuditLogRepository;import br.com.florir.model.AuditLog;import br.com.florir.security.ActorService;import org.springframework.stereotype.Service;
@Service public class AuditService {
 private final AuditLogRepository repository;private final ActorService actor;
 public AuditService(AuditLogRepository r,ActorService a){repository=r;actor=a;}
 public void record(String action,String entity,Long id){if(!"MANAGER".equals(actor.role()))return;var log=new AuditLog();log.setManager(actor.manager());log.setAction(action);log.setEntity(entity);log.setEntityId(id.toString());repository.save(log);}
}
