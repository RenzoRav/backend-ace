package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test", "server.servlet.context-path=/shop"})
class AuthenticationCookieContextIntegrationTest {
  @LocalServerPort int port;

  @Test
  void contextCookieIsSentToRefreshByHttpClient() throws Exception {
    var client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    var login =
        client.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/shop/api/v1/auth/manager/login"))
                .header("Content-Type", "application/json")
                .header("Origin", "http://localhost:3000")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"gestora@loja.local\",\"password\":\"LojaDev123!\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, login.statusCode());
    assertTrue(
        login
            .headers()
            .firstValue("Set-Cookie")
            .orElseThrow()
            .contains("Path=/shop/api/v1/auth/manager"));
    var refresh =
        client.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/shop/api/v1/auth/manager/refresh"))
                .header("Origin", "http://localhost:3000")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, refresh.statusCode());
  }
}
