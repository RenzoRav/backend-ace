package br.com.loja.security;

import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationLimitFilter extends OncePerRequestFilter {
  private final AuthenticationLimitService authenticationLimitService;
  private final SecurityEventService securityEventService;
  private final int loginMaximum, registrationMaximum, refreshMaximum, globalMaximum;

  public AuthenticationLimitFilter(
      AuthenticationLimitService limits, SecurityEventService events, Environment environment) {
    authenticationLimitService = limits;
    securityEventService = events;
    loginMaximum = maximum(environment, "login-ip", 30);
    registrationMaximum = maximum(environment, "register-ip", 5);
    refreshMaximum = maximum(environment, "refresh-ip", 60);
    globalMaximum = maximum(environment, "global", 300);
  }

  private int maximum(Environment environment, String name, int fallback) {
    int value = environment.getProperty("loja.limits." + name, Integer.class, fallback);
    if (value < 1 || value > 100000) throw new IllegalStateException("Limite inválido");
    return value;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String route = RequestPaths.path(request);
    if (!route.startsWith("/api/v1/auth/") || !request.getMethod().equals("POST")) {
      chain.doFilter(request, response);
      return;
    }
    try {
      String scope =
          route.endsWith("/register")
              ? "register-ip"
              : route.endsWith("/refresh") ? "refresh-ip" : "login-ip";
      int maximum =
          scope.equals("register-ip")
              ? registrationMaximum
              : scope.equals("refresh-ip") ? refreshMaximum : loginMaximum;
      Duration window = Duration.ofMinutes(scope.equals("register-ip") ? 10 : 1);
      var global =
          authenticationLimitService.check(
              "global", "application", globalMaximum, Duration.ofMinutes(1));
      var local = authenticationLimitService.check(scope, request.getRemoteAddr(), maximum, window);
      if (!global.allowed() || !local.allowed()) {
        securityEventService.record(
            "RATE_LIMIT", "DENIED", request.getRemoteAddr(), UUID.randomUUID().toString());
        response.setStatus(429);
        response.setHeader(
            "Retry-After",
            Long.toString(
                !global.allowed() ? global.retryAfterSeconds() : local.retryAfterSeconds()));
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Aguarde antes de tentar novamente\"}");
        return;
      }
    } catch (DataAccessException exception) {
      response.setStatus(503);
      response.setContentType("application/json");
      response.getWriter().write("{\"message\":\"Servico temporariamente indisponivel\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
