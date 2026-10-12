package br.com.loja.service;

import br.com.loja.dto.LimitDecision;
import br.com.loja.repository.AuthenticationLimitRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationLimitService {
  private final AuthenticationLimitRepository authenticationLimitRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public LimitDecision check(String scope, String key, int maximum, Duration window) {
    if (maximum < 1
        || maximum > 100000
        || window.isNegative()
        || window.isZero()
        || scope.length() > 64
        || key.length() > 1024)
      throw new IllegalArgumentException("Limites de autenticacao inválidos");
    Instant now = Instant.now();
    return authenticationLimitRepository.consume(
        scope, AuthService.hash(key), maximum, now, now.plus(window));
  }
}
