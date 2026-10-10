package br.com.florir.security;

import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class ActorService {
  private final ManagerRepository managers;
  private final CustomerAccountRepository accounts;

  public ActorService(ManagerRepository m, CustomerAccountRepository a) {
    managers = m;
    accounts = a;
  }

  public Jwt jwt() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt j))
      throw new ApiException(401, "Autenticação necessária");
    return j;
  }

  public String role() {
    return jwt().getClaimAsString("role");
  }

  public Long accountId() {
    return Long.valueOf(jwt().getSubject());
  }

  public Manager manager() {
    if (!"MANAGER".equals(role())) throw new ApiException(403, "Acesso negado");
    return managers.findById(accountId()).orElseThrow(ApiException::missing);
  }

  public Customer customer() {
    if (!"CUSTOMER".equals(role())) throw new ApiException(403, "Acesso negado");
    return accounts.findById(accountId()).orElseThrow(ApiException::missing).getCustomer();
  }

  public void owns(Customer customer) {
    if ("CUSTOMER".equals(role()) && !customer.getId().equals(customer().getId()))
      throw new ApiException(404, "Registro não encontrado");
  }
}
