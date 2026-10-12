package br.com.loja.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class AuthOriginFilter extends OncePerRequestFilter {
  private final String origin;

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (RequestPaths.path(request).startsWith("/api/v1/auth/")
        && !request.getMethod().equals("OPTIONS")
        && !request.getMethod().equals("GET")) {
      String sent = request.getHeader("Origin");
      boolean cookie =
          RequestPaths.path(request).endsWith("/refresh")
              || RequestPaths.path(request).endsWith("/logout");
      if ((sent != null && !origin.equals(sent)) || (cookie && sent == null)) {
        response.setStatus(403);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
