package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFailureTest {
  @Test
  void unavailableLimitStorageFailsClosed() throws Exception {
    var limits = mock(AuthenticationLimitService.class);
    var events = mock(SecurityEventService.class);
    when(limits.check(eq("global"), eq("application"), eq(300), any(Duration.class)))
        .thenThrow(new DataAccessResourceFailureException("private database detail"));
    var response = new MockHttpServletResponse();
    new AuthenticationLimitFilter(limits, events, new MockEnvironment())
        .doFilter(
            new MockHttpServletRequest("POST", "/api/v1/auth/customer/login"),
            response,
            (request, result) -> fail("No authentication on storage failure"));
    assertEquals(503, response.getStatus());
    assertFalse(response.getContentAsString().contains("private database detail"));
  }
}
