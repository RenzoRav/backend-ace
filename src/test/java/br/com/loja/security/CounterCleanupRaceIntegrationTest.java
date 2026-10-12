package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.repository.AuthenticationLimitRepository;
import br.com.loja.service.AuthService;
import br.com.loja.service.AuthenticationLimitService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class CounterCleanupRaceIntegrationTest {
  @Autowired AuthenticationLimitService authenticationLimitService;
  @Autowired AuthenticationLimitRepository authenticationLimitRepository;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired DataSource dataSource;

  @Test
  void cleanupDoesNotDeleteConcurrentlyRenewedCounter() throws Exception {
    String key = UUID.randomUUID().toString(), hash = AuthService.hash(key);
    authenticationLimitService.check("cleanup-race", key, 3, Duration.ofMinutes(5));
    jdbcTemplate.update(
        "update authentication_limit set window_end=now()-interval '1 second' where"
            + " scope='cleanup-race' and key_hash=?",
        hash);
    var executor = Executors.newSingleThreadExecutor();
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (var statement =
          connection.prepareStatement(
              "update authentication_limit set window_end=now()+interval '5 minutes',attempts=3"
                  + " where scope='cleanup-race' and key_hash=?")) {
        statement.setString(1, hash);
        statement.executeUpdate();
      }
      var cleaner =
          executor.submit(() -> authenticationLimitRepository.cleanExpired(Instant.now()));
      boolean waiting = false;
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (System.nanoTime() < deadline) {
        Integer blocked =
            jdbcTemplate.queryForObject(
                "select count(*) from pg_stat_activity where query like 'DELETE FROM"
                    + " authentication_limit%' and wait_event='transactionid'",
                Integer.class);
        if (blocked != null && blocked > 0) {
          waiting = true;
          break;
        }
        Thread.sleep(20);
      }
      assertTrue(waiting, "cleanup must be blocked by renewal transaction");
      connection.commit();
      cleaner.get(5, TimeUnit.SECONDS);
      assertEquals(
          1,
          jdbcTemplate.queryForObject(
              "select count(*) from authentication_limit where scope='cleanup-race' and key_hash=?",
              Integer.class,
              hash));
      assertFalse(
          authenticationLimitService
              .check("cleanup-race", key, 3, Duration.ofMinutes(5))
              .allowed());
    } finally {
      executor.shutdownNow();
    }
  }
}
