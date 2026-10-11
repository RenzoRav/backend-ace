package br.com.loja.security;

import br.com.loja.repository.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class CredentialVersionFilter extends OncePerRequestFilter {
  private final ManagerRepository managerRepository;
  private final CustomerAccountRepository customerAccountRepository;

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwtToken) {
      boolean valid = false;
      try {
        Long id = Long.valueOf(jwtToken.getSubject());
        Long version = jwtToken.getClaim("credentialVersion");
        if ("MANAGER".equals(jwtToken.getClaimAsString("role")))
          valid =
              managerRepository
                  .findById(id)
                  .map(manager -> manager.getActive() && manager.getCredentialVersion() == version)
                  .orElse(false);
        else if ("CUSTOMER".equals(jwtToken.getClaimAsString("role")))
          valid =
              customerAccountRepository
                  .findById(id)
                  .map(customerAccount -> customerAccount.getCredentialVersion() == version)
                  .orElse(false);
      } catch (RuntimeException ignored) {
      }
      if (!valid) {
        SecurityContextHolder.clearContext();
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Sessão expirada\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
