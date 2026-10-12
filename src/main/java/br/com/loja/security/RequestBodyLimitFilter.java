package br.com.loja.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RequestBodyLimitFilter extends OncePerRequestFilter {
  private final int maximumBytes;

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (RequestPaths.path(request).startsWith("/api/v1/auth/"))
      response.setHeader("Cache-Control", "no-store");
    String contentType = request.getContentType();
    if (contentType == null
        || !(contentType.toLowerCase(Locale.ROOT).contains("application/json")
            || contentType.toLowerCase(Locale.ROOT).contains("+json"))) {
      chain.doFilter(request, response);
      return;
    }
    if (request.getContentLengthLong() > maximumBytes) {
      reject(response);
      return;
    }
    byte[] body = request.getInputStream().readNBytes(maximumBytes + 1);
    if (body.length > maximumBytes) {
      reject(response);
      return;
    }
    chain.doFilter(
        new HttpServletRequestWrapper(request) {
          public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
              public int read() {
                return input.read();
              }

              public int read(byte[] buffer, int offset, int length) {
                return input.read(buffer, offset, length);
              }

              public boolean isFinished() {
                return input.available() == 0;
              }

              public boolean isReady() {
                return true;
              }

              public void setReadListener(ReadListener listener) {
                throw new IllegalStateException("Somente leitura sincrona suportada");
              }
            };
          }

          public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
          }
        },
        response);
  }

  private void reject(HttpServletResponse response) throws IOException {
    response.setStatus(413);
    response.setContentType("application/json");
    response.getWriter().write("{\"message\":\"Corpo da requisicao excede o limite\"}");
  }
}
