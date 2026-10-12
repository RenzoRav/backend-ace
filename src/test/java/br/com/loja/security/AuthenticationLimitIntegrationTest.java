package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.service.AuthService;
import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class AuthenticationLimitIntegrationTest {
  @Autowired AuthenticationLimitService authenticationLimitService;
  @Autowired SecurityEventService securityEventService;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void enforcesThresholdAndIndependentKeys() {
    String key = UUID.randomUUID().toString();
    for (int attempt = 0; attempt < 3; attempt++)
      assertTrue(authenticationLimitService.check("test", key, 3, Duration.ofMinutes(1)).allowed());
    var decision = authenticationLimitService.check("test", key, 3, Duration.ofMinutes(1));
    assertFalse(decision.allowed());
    assertTrue(decision.retryAfterSeconds() > 0);
    assertTrue(
        authenticationLimitService
            .check("test", UUID.randomUUID().toString(), 3, Duration.ofMinutes(1))
            .allowed());
  }

  @Test
  void concurrentRequestsNeverExceedBudget() throws Exception {
    String key = UUID.randomUUID().toString();
    var executor = Executors.newFixedThreadPool(8);
    try {
      var futures = new ArrayList<Future<Boolean>>();
      for (int attempt = 0; attempt < 20; attempt++)
        futures.add(
            executor.submit(
                () ->
                    authenticationLimitService
                        .check("parallel", key, 5, Duration.ofMinutes(1))
                        .allowed()));
      int allowed = 0;
      for (var future : futures) if (future.get()) allowed++;
      assertEquals(5, allowed);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void securityEventsDoNotStoreCredentials() {
    String correlation = UUID.randomUUID().toString();
    securityEventService.record("LOGIN", "DENIED", "unknown", correlation);
    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "select count(*) from security_event where correlation_id=? and event='LOGIN' and"
                + " result='DENIED'",
            Integer.class,
            correlation));
  }

  @Test
  void expiredLimitResetsBudget() {
    String key = UUID.randomUUID().toString();
    assertTrue(authenticationLimitService.check("expiry", key, 1, Duration.ofMinutes(1)).allowed());
    assertFalse(
        authenticationLimitService.check("expiry", key, 1, Duration.ofMinutes(1)).allowed());
    jdbcTemplate.update(
        "update authentication_limit set window_end=now()-interval '1 second' where scope='expiry'"
            + " and key_hash=?",
        AuthService.hash(key));
    assertTrue(authenticationLimitService.check("expiry", key, 1, Duration.ofMinutes(1)).allowed());
  }

  @Test
  void securityEventPersistsWhenBusinessTransactionRollsBack() {
    String correlation = UUID.randomUUID().toString();
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(transactionManager)
                .execute(
                    status -> {
                      securityEventService.record(
                          "LOGIN", "DENIED", "person@example.invalid", correlation);
                      throw new IllegalStateException("rollback fixture");
                    }));
    String actor =
        jdbcTemplate.queryForObject(
            "select actor_reference from security_event where correlation_id=?",
            String.class,
            correlation);
    assertEquals(AuthService.hash("person@example.invalid"), actor);
  }
}
