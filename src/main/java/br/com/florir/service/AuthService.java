package br.com.florir.service;

import br.com.florir.dto.AuthDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
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
  private final ManagerRepository managers;
  private final CustomerAccountRepository accounts;
  private final CustomerRepository customers;
  private final RefreshSessionRepository sessions;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final String issuer, audience;
  private final String dummyHash;
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

  public AuthService(
      ManagerRepository m,
      CustomerAccountRepository a,
      CustomerRepository c,
      RefreshSessionRepository s,
      PasswordEncoder p,
      JwtEncoder e,
      @Value("$" + "{florir.jwt.issuer}") String i,
      @Value("$" + "{florir.jwt.audience}") String aud) {
    managers = m;
    accounts = a;
    customers = c;
    sessions = s;
    passwords = p;
    encoder = e;
    issuer = i;
    audience = aud;
    dummyHash = p.encode(UUID.randomUUID().toString());
  }

  public record Result(Session session, String refreshToken) {}

  private String email(String e) {
    return e.strip().toLowerCase(Locale.ROOT);
  }

  public Result login(String role, Login login) {
    if ("MANAGER".equals(role)) {
      var m = managers.findByEmail(email(login.email())).orElse(null);
      boolean matches =
          passwords.matches(login.password(), m == null ? dummyHash : m.getPasswordHash());
      if (m == null || !m.getActive() || !matches)
        throw new ApiException(401, "Credenciais inválidas");
      return issue(role, m.getId(), m.getName(), m.getCredentialVersion());
    }
    var a = accounts.findByEmail(email(login.email())).orElse(null);
    boolean matches =
        passwords.matches(login.password(), a == null ? dummyHash : a.getPasswordHash());
    if (a == null || !matches) throw new ApiException(401, "Credenciais inválidas");
    return issue(role, a.getId(), a.getCustomer().getName(), a.getCredentialVersion());
  }

  public Result register(Register data) {
    String phone = PhoneNumbers.normalize(data.phone());
    String mail = email(data.email());
    if (accounts.existsByEmail(mail) || customers.existsByPhone(phone))
      throw ApiException.conflict("E-mail ou telefone já cadastrado");
    var c = new Customer();
    c.setName(data.name().strip());
    c.setPhone(phone);
    c.setBirthday(data.birthday());
    customers.save(c);
    var a = new CustomerAccount();
    a.setCustomer(c);
    a.setEmail(mail);
    a.setPasswordHash(passwords.encode(data.password()));
    accounts.save(a);
    return issue("CUSTOMER", a.getId(), c.getName(), a.getCredentialVersion());
  }

  private Result issue(String role, Long id, String name, long version) {
    Instant now = Instant.now();
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
            .build();
    String jwt =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var refresh = new RefreshSession();
    refresh.setRole(role);
    refresh.setAccountId(id);
    refresh.setCredentialVersion(version);
    refresh.setTokenHash(hash(raw));
    refresh.setExpiresAt(now.plusSeconds(604800));
    sessions.save(refresh);
    return new Result(new Session(jwt, "Bearer", 900, role, id.toString(), name), raw);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean validSession(RefreshSession s, String role, long version) {
    return role.equals(s.getRole())
        && !s.getRevoked()
        && s.getExpiresAt().isAfter(Instant.now())
        && s.getCredentialVersion() == version;
  }

  public Result refresh(String role, String raw) {
    if (raw == null || raw.isBlank()) throw new ApiException(401, "Sessão expirada");
    var session =
        sessions
            .findByTokenHash(hash(raw))
            .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
    if (!role.equals(session.getRole())) throw new ApiException(401, "Sessão expirada");
    if ("MANAGER".equals(role)) {
      var account =
          managers
              .lockById(session.getAccountId())
              .filter(Manager::getActive)
              .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
      entityManager.refresh(session, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
      if (!validSession(session, role, account.getCredentialVersion()))
        throw new ApiException(401, "Sessão expirada");
      session.setRevoked(true);
      return issue(role, account.getId(), account.getName(), account.getCredentialVersion());
    }
    var account =
        accounts
            .lockById(session.getAccountId())
            .orElseThrow(() -> new ApiException(401, "Sessão expirada"));
    entityManager.refresh(session, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    if (!validSession(session, role, account.getCredentialVersion()))
      throw new ApiException(401, "Sessão expirada");
    session.setRevoked(true);
    return issue(
        role, account.getId(), account.getCustomer().getName(), account.getCredentialVersion());
  }

  public void logout(String role, String raw) {
    if (raw == null || raw.isBlank()) return;
    var optional = sessions.findByTokenHash(hash(raw));
    if (optional.isEmpty()) return;
    var session = optional.get();
    if (!role.equals(session.getRole())
        || session.getRevoked()
        || session.getExpiresAt().isBefore(Instant.now())) return;
    long version;
    if ("MANAGER".equals(role)) {
      var account = managers.lockById(session.getAccountId());
      if (account.isEmpty()) return;
      version = account.get().getCredentialVersion();
    } else {
      var account = accounts.lockById(session.getAccountId());
      if (account.isEmpty()) return;
      version = account.get().getCredentialVersion();
    }
    entityManager.refresh(session, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    if (validSession(session, role, version)) revoke(role, session.getAccountId());
  }

  public void revoke(String role, Long id) {
    if ("MANAGER".equals(role)) {
      var account = managers.lockById(id).orElseThrow(ApiException::missing);
      account.setCredentialVersion(account.getCredentialVersion() + 1);
    } else {
      var account = accounts.lockById(id).orElseThrow(ApiException::missing);
      account.setCredentialVersion(account.getCredentialVersion() + 1);
    }
    sessions.findByRoleAndAccountId(role, id).forEach(s -> s.setRevoked(true));
  }

  public void changePassword(String role, Long id, PasswordChange data) {
    if ("MANAGER".equals(role)) {
      var account = managers.lockById(id).orElseThrow(ApiException::missing);
      if (!passwords.matches(data.currentPassword(), account.getPasswordHash()))
        throw new ApiException(401, "Senha atual inválida");
      account.setPasswordHash(passwords.encode(data.newPassword()));
    } else {
      var account = accounts.lockById(id).orElseThrow(ApiException::missing);
      if (!passwords.matches(data.currentPassword(), account.getPasswordHash()))
        throw new ApiException(401, "Senha atual inválida");
      account.setPasswordHash(passwords.encode(data.newPassword()));
    }
    revoke(role, id);
  }
}
