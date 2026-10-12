package br.com.loja.security;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestPaths {
  private RequestPaths() {}

  public static String path(HttpServletRequest request) {
    String servletPath = request.getServletPath();
    if (servletPath != null && !servletPath.isEmpty()) return servletPath;
    String uri = request.getRequestURI(), context = request.getContextPath();
    return context != null && !context.isEmpty() && uri.startsWith(context)
        ? uri.substring(context.length())
        : uri;
  }
}
