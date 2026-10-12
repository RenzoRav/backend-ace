package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.exception.ApiException;
import br.com.loja.service.AuthService;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.profiles.active=test")
class SessionFamilyIntegrationTest {
  @Autowired AuthService authService;
  @Autowired JdbcTemplate jdbcTemplate;

  AuthService.Result login() {
    return authService.login("MANAGER", new AuthDtos.Login("gestora@loja.local", "LojaDev123!"));
  }

  @Test
  void replayRevokesSuccessorAndKeepsOtherLoginValid() {
    var first = login();
    var independent = login();
    var successor = authService.refresh("MANAGER", first.refreshToken());
    assertThrows(ApiException.class, () -> authService.refresh("MANAGER", first.refreshToken()));
    assertThrows(
        ApiException.class, () -> authService.refresh("MANAGER", successor.refreshToken()));
    assertDoesNotThrow(() -> authService.refresh("MANAGER", independent.refreshToken()));
  }

  @Test
  void absoluteExpirationRejectsRefresh() {
    var first = login();
    jdbcTemplate.update(
        "update session_family set absolute_expires_at=now()-interval '1 second' where id=(select"
            + " family_id from refresh_session where token_hash=?)",
        AuthService.hash(first.refreshToken()));
    assertThrows(ApiException.class, () -> authService.refresh("MANAGER", first.refreshToken()));
  }

  @Test
  void inactivityExpirationRejectsRefresh() {
    var first = login();
    jdbcTemplate.update(
        "update refresh_session set expires_at=now()-interval '1 second' where token_hash=?",
        AuthService.hash(first.refreshToken()));
    assertThrows(ApiException.class, () -> authService.refresh("MANAGER", first.refreshToken()));
  }

  @Test
  void simultaneousRefreshCannotLeaveReplayFamilyUsable() throws Exception {
    var first = login();
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    Callable<AuthService.Result> operation =
        () -> {
          start.await();
          try {
            return authService.refresh("MANAGER", first.refreshToken());
          } catch (ApiException exception) {
            return null;
          }
        };
    try {
      var left = executor.submit(operation);
      var right = executor.submit(operation);
      start.countDown();
      var firstResult = left.get(10, TimeUnit.SECONDS);
      var secondResult = right.get(10, TimeUnit.SECONDS);
      assertNotEquals(firstResult == null, secondResult == null);
      var winner = firstResult == null ? secondResult : firstResult;
      assertThrows(ApiException.class, () -> authService.refresh("MANAGER", winner.refreshToken()));
    } finally {
      executor.shutdownNow();
    }
  }
}
