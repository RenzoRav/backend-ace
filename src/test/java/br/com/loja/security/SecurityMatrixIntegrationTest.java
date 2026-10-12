package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.service.AuthService;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test", "loja.limits.login-account=3"})
class SecurityMatrixIntegrationTest {
  @LocalServerPort int port;
  @Autowired AuthService authService;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired JwtDecoder jwtDecoder;
  @Autowired JdbcTemplate jdbcTemplate;
  final HttpClient client = HttpClient.newHttpClient();

  HttpResponse<String> call(String method, String path, String body, String token, String origin)
      throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
    if (token != null) builder.header("Authorization", "Bearer " + token);
    if (origin != null) builder.header("Origin", origin);
    if (body != null) builder.header("Content-Type", "application/json");
    return client.send(
        builder
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  AuthService.Result manager() {
    return authService.login("MANAGER", new AuthDtos.Login("gestora@loja.local", "LojaDev123!"));
  }

  @Test
  void rejectsForeignAndMissingOrigin() throws Exception {
    assertEquals(403, call("POST", "/auth/customer/refresh", null, null, null).statusCode());
    assertEquals(
        403,
        call("POST", "/auth/customer/refresh", null, null, "https://foreign.example").statusCode());
  }

  @Test
  void rejectsInvalidJwtAndRoles() throws Exception {
    assertEquals(401, call("GET", "/manager/products", null, "invalid", null).statusCode());
    assertEquals(
        403,
        call("GET", "/customer/me", null, manager().session().accessToken(), null).statusCode());
  }

  @Test
  void rejectsSignedWrongIssuerAudienceAndExpiredToken() throws Exception {
    var original = jwtDecoder.decode(manager().session().accessToken());
    for (String failure :
        List.of(
            "issuer",
            "audience",
            "expiry",
            "missing-audience",
            "missing-expiry",
            "missing-issuer")) {
      var claims = JwtClaimsSet.builder().claims(values -> values.putAll(original.getClaims()));
      if (failure.equals("missing-audience")) claims.claims(values -> values.remove("aud"));
      if (failure.equals("missing-expiry")) claims.claims(values -> values.remove("exp"));
      if (failure.equals("missing-issuer")) claims.claims(values -> values.remove("iss"));
      if (failure.equals("issuer")) claims.issuer("wrong");
      if (failure.equals("audience")) claims.audience(List.of("wrong"));
      if (failure.equals("expiry"))
        claims
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600));
      String token =
          jwtEncoder
              .encode(
                  JwtEncoderParameters.from(
                      JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
              .getTokenValue();
      assertEquals(401, call("GET", "/manager/products", null, token, null).statusCode(), failure);
    }
  }

  @Test
  void replayRevokesAccessToken() throws Exception {
    var first = manager();
    var successor = authService.refresh("MANAGER", first.refreshToken());
    assertThrows(
        RuntimeException.class, () -> authService.refresh("MANAGER", first.refreshToken()));
    assertEquals(
        401,
        call("GET", "/manager/products", null, successor.session().accessToken(), null)
            .statusCode());
  }

  @Test
  void inactiveManagerCannotUseExistingToken() throws Exception {
    var result = manager();
    long account = Long.parseLong(result.session().accountId());
    try {
      jdbcTemplate.update("update manager set active=false where id=?", account);
      assertEquals(
          401,
          call("GET", "/manager/products", null, result.session().accessToken(), null)
              .statusCode());
    } finally {
      jdbcTemplate.update("update manager set active=true where id=?", account);
    }
  }

  @Test
  void rejectsOversizedAndMalformedJson() throws Exception {
    assertEquals(
        413, call("POST", "/auth/customer/login", "x".repeat(262145), null, null).statusCode());
    assertEquals(400, call("POST", "/auth/customer/login", "{", null, null).statusCode());
  }

  @Test
  void accountLimiterReturnsRetryAfterAndNoStore() throws Exception {
    String email = "throttle-" + System.nanoTime() + "@example.invalid";
    String body = "{\"email\":\"" + email + "\",\"password\":\"InvalidPassword!\"}";
    for (int attempt = 0; attempt < 3; attempt++)
      assertEquals(401, call("POST", "/auth/customer/login", body, null, null).statusCode());
    var response = call("POST", "/auth/customer/login", body, null, null);
    assertEquals(429, response.statusCode());
    assertTrue(
        jdbcTemplate.queryForObject(
                "select count(*) from security_event where event=\u0027RATE_LIMIT\u0027 and"
                    + " result=\u0027DENIED\u0027 and actor_reference=?",
                Integer.class,
                AuthService.hash("customer:" + email))
            > 0);
    assertTrue(response.headers().firstValue("Retry-After").isPresent());
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
  }

  @Test
  void rejectsTamperedSignatureAndUnsignedAlgorithm() throws Exception {
    String token = manager().session().accessToken();
    String[] parts = token.split("\\.");
    assertEquals(
        401, call("GET", "/manager/products", null, token + "tampered", null).statusCode());
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    assertEquals(
        401,
        call("GET", "/manager/products", null, header + "." + parts[1] + ".", null).statusCode());
  }

  @Test
  void rejectedRefreshIsAudited() throws Exception {
    int before =
        jdbcTemplate.queryForObject(
            "select count(*) from security_event where event='REFRESH' and result='DENIED'",
            Integer.class);
    assertEquals(
        401,
        call("POST", "/auth/customer/refresh", null, null, "http://localhost:3000").statusCode());
    assertTrue(
        jdbcTemplate.queryForObject(
                "select count(*) from security_event where event='REFRESH' and result='DENIED'",
                Integer.class)
            > before);
  }
}
