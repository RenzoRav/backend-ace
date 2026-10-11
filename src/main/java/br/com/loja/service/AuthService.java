package br.com.loja.service;

import br.com.loja.dto.AuthDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
  private final ManagerRepository managerRepository;
  private final CustomerAccountRepository customerAccountRepository;
  private final CustomerRepository customerRepository;
  private final RefreshSessionRepository refreshSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionFamilyService sessionFamilyService;
  private final SessionFamilyRepository sessionFamilyRepository;
  private final SecurityEventService securityEventService;
  private final JwtEncoder jwtEncoder;
  private final String issuer, audience;
  private final String dummyHash;
  @PersistenceContext private EntityManager entityManager;

  public AuthService(
      ManagerRepository managerRepository,
      CustomerAccountRepository customerAccountRepository,
      CustomerRepository customerRepository,
      RefreshSessionRepository refreshSessionRepository,
      PasswordEncoder passwordEncoder,
      SessionFamilyService sessionFamilyService,
      SessionFamilyRepository sessionFamilyRepository,
      SecurityEventService securityEventService,
      JwtEncoder jwtEncoder,
      @Value("$" + "{loja.jwt.issuer}") String issuer,
      @Value("$" + "{loja.jwt.audience}") String audience) {
    this.managerRepository = managerRepository;
    this.customerAccountRepository = customerAccountRepository;
    this.customerRepository = customerRepository;
    this.refreshSessionRepository = refreshSessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.sessionFamilyService=sessionFamilyService;
    this.sessionFamilyRepository=sessionFamilyRepository;
    this.securityEventService=securityEventService;
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.audience = audience;
    dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  public record Result(Session session, String refreshToken) {}

  private String email(String emailAddress) {
    return emailAddress.strip().toLowerCase(Locale.ROOT);
  }

  public Result login(String role, Login login) {
    if ("MANAGER".equals(role)) {
      var manager = managerRepository.findByEmail(email(login.email())).orElse(null);
      boolean matches =
          passwordEncoder.matches(
              login.password(), manager == null ? dummyHash : manager.getPasswordHash());
      if (manager == null || !manager.getActive() || !matches)
        throw new ApiException(401, "Credenciais inválidas");
      return issue(role, manager.getId(), manager.getName(), manager.getCredentialVersion());
    }
    var customerAccount = customerAccountRepository.findByEmail(email(login.email())).orElse(null);
    boolean matches =
        passwordEncoder.matches(
            login.password(),
            customerAccount == null ? dummyHash : customerAccount.getPasswordHash());
    if (customerAccount == null || !matches) throw new ApiException(401, "Credenciais inválidas");
    return issue(
        role,
        customerAccount.getId(),
        customerAccount.getCustomer().getName(),
        customerAccount.getCredentialVersion());
  }

  public Result register(Register data) {
    String phone = PhoneNumbers.normalize(data.phone());
    String mail = email(data.email());
    if (customerAccountRepository.existsByEmail(mail) || customerRepository.existsByPhone(phone))
      throw ApiException.conflict("E-mail ou telefone já cadastrado");
    var customer = new Customer();
    customer.setName(data.name().strip());
    customer.setPhone(phone);
    customer.setBirthday(data.birthday());
    customerRepository.save(customer);
    var customerAccount = new CustomerAccount();
    customerAccount.setCustomer(customer);
    customerAccount.setEmail(mail);
    customerAccount.setPasswordHash(passwordEncoder.encode(data.password()));
    customerAccountRepository.save(customerAccount);
    return issue(
        "CUSTOMER",
        customerAccount.getId(),
        customer.getName(),
        customerAccount.getCredentialVersion());
  }

  private Result issue(String role, Long id, String name, long version) {
    return issue(role,id,name,version,sessionFamilyService.create(role,id,Instant.now()));
  }

  private Result issue(String role,Long id,String name,long version,SessionFamily family){
    Instant now=Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .id(UUID.randomUUID().toString())
            .claim("role", role)
            .claim("credentialVersion", version)
            .claim("sessionFamilyId",family.getId())
            .build();
    String accessToken =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var refresh = new RefreshSession();
    refresh.setRole(role);
    refresh.setAccountId(id);
    refresh.setCredentialVersion(version);
    refresh.setTokenHash(hash(refreshToken));
    refresh.setFamilyId(family.getId());
    Instant inactivityExpiry=now.plusSeconds(604800);
    refresh.setExpiresAt(inactivityExpiry.isBefore(family.getAbsoluteExpiresAt())?inactivityExpiry:family.getAbsoluteExpiresAt());
    refreshSessionRepository.save(refresh);
    return new Result(
        new Session(accessToken, "Bearer", 900, role, id.toString(), name), refreshToken);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private boolean validSession(RefreshSession refreshSession, String role, long version) {
    return role.equals(refreshSession.getRole())
        && !refreshSession.getRevoked()
        && refreshSession.getExpiresAt().isAfter(Instant.now())
        && refreshSession.getCredentialVersion() == version
        && sessionFamilyService.isActive(refreshSession.getFamilyId(),role,refreshSession.getAccountId(),Instant.now());
  }

  @Transactional(noRollbackFor=ApiException.class)
  public Result refresh(String role, String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank())
      throw new ApiException(401, "Sessão expirada");
    var session =
        refreshSessionRepository
            .findByTokenHash(hash(refreshToken))
            .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
    if (!role.equals(session.getRole())) throw new ApiException(401, "Sessão expirada");
    if ("MANAGER".equals(role)) {
      var account =
          managerRepository
              .lockById(session.getAccountId())
              .filter(Manager::getActive)
              .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
      entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
      SessionFamily family=checkFamily(session,role);
    if (!validSession(session, role, account.getCredentialVersion()))
        throw new ApiException(401, "Sessão expirada");
      session.setRevoked(true);
      return issue(role, account.getId(), account.getName(), account.getCredentialVersion(),family);
    }
    var account =
        customerAccountRepository
            .lockById(session.getAccountId())
            .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
    entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
    SessionFamily family=checkFamily(session,role);
    if (!validSession(session, role, account.getCredentialVersion()))
      throw new ApiException(401, "Sessão expirada");
    session.setRevoked(true);
    return issue(
        role, account.getId(), account.getCustomer().getName(), account.getCredentialVersion(),family);
  }

  private SessionFamily checkFamily(RefreshSession session,String role){
    if(session.getFamilyId()==null)throw new ApiException(401,"Sessao expirada");
    var family=sessionFamilyRepository.lockById(session.getFamilyId()).orElseThrow(()->new ApiException(401,"Sessao expirada"));
    if(!role.equals(family.getRole())||!session.getAccountId().equals(family.getAccountId()))throw new ApiException(401,"Sessao expirada");
    if(session.getRevoked()){
      family.setRevoked(true);
      securityEventService.record("REPLAY","REVOKED",role+":"+session.getAccountId(),UUID.randomUUID().toString());
      throw new ApiException(401,"Sessao expirada");
    }
    if(family.getRevoked()||!family.getAbsoluteExpiresAt().isAfter(Instant.now()))throw new ApiException(401,"Sessao expirada");
    return family;
  }

  public void logout(String role, String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) return;
    var optional = refreshSessionRepository.findByTokenHash(hash(refreshToken));
    if (optional.isEmpty()) return;
    var session = optional.get();
    if (!role.equals(session.getRole())
        || session.getRevoked()
        || session.getExpiresAt().isBefore(Instant.now())) return;
    long version;
    if ("MANAGER".equals(role)) {
      var account = managerRepository.lockById(session.getAccountId());
      if (account.isEmpty()) return;
      version = account.get().getCredentialVersion();
    } else {
      var account = customerAccountRepository.lockById(session.getAccountId());
      if (account.isEmpty()) return;
      version = account.get().getCredentialVersion();
    }
    entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
    if (validSession(session, role, version)) revoke(role, session.getAccountId());
  }

  public void revoke(String role, Long id) {
    if ("MANAGER".equals(role)) {
      var account = managerRepository.lockById(id).orElseThrow(ApiException::missing);
      account.setCredentialVersion(account.getCredentialVersion() + 1);
    } else {
      var account = customerAccountRepository.lockById(id).orElseThrow(ApiException::missing);
      account.setCredentialVersion(account.getCredentialVersion() + 1);
    }
    sessionFamilyRepository.findByRoleAndAccountId(role,id).forEach(family->family.setRevoked(true));
    refreshSessionRepository
        .findByRoleAndAccountId(role, id)
        .forEach(refreshSession -> refreshSession.setRevoked(true));
  }

  public void changePassword(String role, Long id, PasswordChange data) {
    if ("MANAGER".equals(role)) {
      var account = managerRepository.lockById(id).orElseThrow(ApiException::missing);
      if (!passwordEncoder.matches(data.currentPassword(), account.getPasswordHash()))
        throw new ApiException(401, "Senha atual inválida");
      account.setPasswordHash(passwordEncoder.encode(data.newPassword()));
    } else {
      var account = customerAccountRepository.lockById(id).orElseThrow(ApiException::missing);
      if (!passwordEncoder.matches(data.currentPassword(), account.getPasswordHash()))
        throw new ApiException(401, "Senha atual inválida");
      account.setPasswordHash(passwordEncoder.encode(data.newPassword()));
    }
    revoke(role, id);
  }
}


