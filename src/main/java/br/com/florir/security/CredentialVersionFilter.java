package br.com.florir.security;
import br.com.florir.repository.*;import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.oauth2.jwt.Jwt;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class CredentialVersionFilter extends OncePerRequestFilter {
 private final ManagerRepository managers;private final CustomerAccountRepository accounts;
 public CredentialVersionFilter(ManagerRepository m,CustomerAccountRepository a){managers=m;accounts=a;}
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null && auth.getPrincipal() instanceof Jwt jwt){
   boolean valid=false;try {Long id=Long.valueOf(jwt.getSubject());Long version=jwt.getClaim("credentialVersion");if("MANAGER".equals(jwt.getClaimAsString("role")))valid=managers.findById(id).map(m->m.getActive()&&m.getCredentialVersion()==version).orElse(false);else if("CUSTOMER".equals(jwt.getClaimAsString("role")))valid=accounts.findById(id).map(a->a.getCredentialVersion()==version).orElse(false);}catch(RuntimeException ignored){}
   if(!valid){SecurityContextHolder.clearContext();response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"message\":\"Sessão expirada\"}");return;}
  }chain.doFilter(request,response);
 }
}
