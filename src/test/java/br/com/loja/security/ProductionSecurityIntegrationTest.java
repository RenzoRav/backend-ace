package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.service.AuthService;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.profiles.active=prod",
      "spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/loja_test?currentSchema=production_security_test",
      "spring.datasource.password=loja_dev",
      "spring.flyway.schemas=production_security_test",
      "spring.flyway.default-schema=production_security_test",
      "spring.jpa.properties.hibernate.default_schema=production_security_test",
      "loja.jwt.secret=faac9c65393770bd7ea050a031e508e2f8a7a274b0a8f58502fba4de31c70dac",
      "loja.allowed-origin=https://shop.example",
      "loja.bootstrap.enabled=false"
    })
class ProductionSecurityIntegrationTest {
  @LocalServerPort int port;
  @Autowired AuthService authService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void forbidsPaymentSimulationWithoutBusinessChanges() throws Exception {
    var result =
        authService.register(
            new AuthDtos.Register(
                "Production checks",
                "production-" + System.nanoTime() + "@example.invalid",
                "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999),
                "ProductionPass123!",
                null));
    int before =
        jdbcTemplate.queryForObject("select count(*) from financial_movement", Integer.class);
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + port
                                + "/api/v1/customer/orders/1/simulate-payment"))
                    .header("Authorization", "Bearer " + result.session().accessToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(403, response.statusCode());
    assertEquals(
        before,
        jdbcTemplate.queryForObject("select count(*) from financial_movement", Integer.class));
  }

  @Test
  void productionCookiesAreSecureHttpOnlyAndStrict() throws Exception {
    String email = "cookie-" + System.nanoTime() + "@example.invalid";
    String phone = "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    String body =
        "{\"name\":\"Cookie checks\",\"email\":\""
            + email
            + "\",\"phone\":\""
            + phone
            + "\",\"password\":\"CookiePass123!\"}";
    int before =
        jdbcTemplate.queryForObject("select count(*) from financial_movement", Integer.class);
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/auth/customer/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(201, response.statusCode());
    String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(cookie.contains("Secure"));
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
  }
}
