package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.exception.ApiException;
import br.com.loja.service.AuthService;
import br.com.loja.service.SecurityEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(properties = "spring.profiles.active=test")
class ReplayAuditFailureIntegrationTest {
  @Autowired AuthService authService;
  @MockitoSpyBean SecurityEventService securityEventService;

  @Test
  void replayRevocationSurvivesAuditStorageFailure() {
    var first =
        authService.login("MANAGER", new AuthDtos.Login("gestora@loja.local", "LojaDev123!"));
    var successor = authService.refresh("MANAGER", first.refreshToken());
    doThrow(new DataAccessResourceFailureException("audit fixture unavailable"))
        .when(securityEventService)
        .record(eq("REPLAY"), eq("REVOKED"), anyString(), anyString());
    assertThrows(
        RuntimeException.class, () -> authService.refresh("MANAGER", first.refreshToken()));
    assertThrows(
        ApiException.class, () -> authService.refresh("MANAGER", successor.refreshToken()));
  }
}
