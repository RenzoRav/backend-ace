package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationPathTest {
  @Test
  void originCheckUsesDecodedServletPathWithinContext() throws Exception {
    var request = new MockHttpServletRequest("POST", "/shop/api/v1/auth/customer/%72efresh");
    request.setContextPath("/shop");
    request.setServletPath("/api/v1/auth/customer/refresh");
    var response = new MockHttpServletResponse();
    new AuthOriginFilter("http://localhost:3000")
        .doFilter(
            request, response, (incoming, outgoing) -> fail("missing Origin must reject refresh"));
    assertEquals(403, response.getStatus());
  }

  @Test
  void noStoreAppliesInsideServletContext() throws Exception {
    var request = new MockHttpServletRequest("POST", "/shop/api/v1/auth/customer/login");
    request.setContextPath("/shop");
    request.setServletPath("/api/v1/auth/customer/login");
    var response = new MockHttpServletResponse();
    new RequestBodyLimitFilter(262144).doFilter(request, response, (incoming, outgoing) -> {});
    assertEquals("no-store", response.getHeader("Cache-Control"));
  }
}
