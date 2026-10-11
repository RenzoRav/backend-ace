package br.com.loja.security;

import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActorService {
  private final ManagerRepository managerRepository;
  private final CustomerAccountRepository customerAccountRepository;

  public Jwt jwt() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwtToken))
      throw new ApiException(401, "Autenticação necessária");
    return jwtToken;
  }

  public String role() {
    return jwt().getClaimAsString("role");
  }

  public Long accountId() {
    return Long.valueOf(jwt().getSubject());
  }

  public Manager manager() {
    if (!"MANAGER".equals(role())) throw new ApiException(403, "Acesso negado");
    return managerRepository.findById(accountId()).orElseThrow(ApiException::missing);
  }

  public Customer customer() {
    if (!"CUSTOMER".equals(role())) throw new ApiException(403, "Acesso negado");
    return customerAccountRepository
        .findById(accountId())
        .orElseThrow(ApiException::missing)
        .getCustomer();
  }

  public void owns(Customer customer) {
    if ("CUSTOMER".equals(role()) && !customer.getId().equals(customer().getId()))
      throw new ApiException(404, "Registro não encontrado");
  }
}
