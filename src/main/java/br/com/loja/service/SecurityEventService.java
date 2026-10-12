package br.com.loja.service;

import br.com.loja.model.SecurityEvent;
import br.com.loja.repository.SecurityEventRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityEventService {
  private final SecurityEventRepository securityEventRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String event, String result, String actorReference, String correlationId) {
    if (!Set.of("LOGIN", "REGISTER", "REFRESH", "REPLAY", "LOGOUT", "PASSWORD", "RATE_LIMIT")
            .contains(event)
        || !Set.of("ALLOWED", "DENIED", "REVOKED").contains(result))
      throw new IllegalArgumentException("Evento desconhecido");
    var securityEvent = new SecurityEvent();
    securityEvent.setEvent(event);
    securityEvent.setResult(result);
    securityEvent.setActorReference(AuthService.hash(actorReference));
    securityEvent.setCorrelationId(
        correlationId.length() <= 64 ? correlationId : AuthService.hash(correlationId));
    securityEvent.setCreatedAt(Instant.now());
    securityEventRepository.save(securityEvent);
  }
}
