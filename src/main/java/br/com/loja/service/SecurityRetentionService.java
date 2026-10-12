package br.com.loja.service;

import br.com.loja.repository.AuthenticationLimitRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityRetentionService {
  private final AuthenticationLimitRepository authenticationLimitRepository;
  private final JdbcTemplate jdbcTemplate;

  @Scheduled(
      fixedDelayString = "${loja.security.cleanup-milliseconds:600000}",
      initialDelay = 600000)
  public void clean() {
    Instant now = Instant.now();
    authenticationLimitRepository.cleanExpired(now);
    jdbcTemplate.update(
        "DELETE FROM security_event WHERE id IN (SELECT id FROM security_event WHERE created_at<?"
            + " ORDER BY id LIMIT 1000)",
        Timestamp.from(now.minus(Duration.ofDays(30))));
  }
}
