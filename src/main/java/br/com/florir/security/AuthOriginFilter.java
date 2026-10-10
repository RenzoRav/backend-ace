package br.com.florir.security;
import org.springframework.web.filter.OncePerRequestFilter;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class AuthOriginFilter extends OncePerRequestFilter {
 private final String origin;public AuthOriginFilter(String origin){this.origin=origin;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(req.getRequestURI().startsWith("/api/v1/auth/") && !req.getMethod().equals("OPTIONS") && !req.getMethod().equals("GET")) {
   String sent=req.getHeader("Origin");boolean cookie=req.getRequestURI().endsWith("/refresh")||req.getRequestURI().endsWith("/logout");
   if((sent!=null&&!origin.equals(sent))||(cookie&&sent==null)){res.setStatus(403);return;}
  }chain.doFilter(req,res);
 }
}
