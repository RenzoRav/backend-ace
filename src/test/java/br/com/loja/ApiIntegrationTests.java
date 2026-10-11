package br.com.loja;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTests {
  @LocalServerPort int port;
  final HttpClient httpClient =
      HttpClient.newBuilder()
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .build();
  String manager;
  String customer;

  record Reply(int status, String body) {}

  Reply request(String method, String route, String json, String token) throws Exception {
    var requestBuilder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + route))
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000");
    if (token != null) requestBuilder.header("Authorization", "Bearer " + token);
    var response =
        httpClient.send(
            requestBuilder
                .method(
                    method,
                    json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return new Reply(response.statusCode(), response.body());
  }

  String value(String body, String fieldName) {
    var matcher =
        java.util.regex.Pattern.compile("\\\"" + fieldName + "\\\":\\\"([^\\\"]+)\\\"")
            .matcher(body);
    assertTrue(matcher.find(), body);
    return matcher.group(1);
  }

  String managerLogin() throws Exception {
    var response =
        request(
            "POST",
            "/auth/manager/login",
            "{\"email\":\"gestora@loja.local\",\"password\":\"LojaDev123!\"}",
            null);
    assertEquals(200, response.status(), response.body());
    return value(response.body(), "accessToken");
  }

  @Test
  void realAuthenticationAndAuthorization() throws Exception {
    assertEquals(401, request("GET", "/manager/products", null, null).status());
    assertEquals(
        401,
        request(
                "POST",
                "/auth/manager/login",
                "{\"email\":\"gestora@loja.local\",\"password\":\"wrong\"}",
                null)
            .status());
    manager = managerLogin();
    assertEquals(200, request("GET", "/manager/products", null, manager).status());
    String suffix = String.valueOf(System.nanoTime());
    var signup =
        request(
            "POST",
            "/auth/customer/register",
            "{\"name\":\"Teste\",\"email\":\"t"
                + suffix
                + "@example.com\",\"phone\":\"119"
                + suffix.substring(suffix.length() - 8)
                + "\",\"password\":\"TestPass123!\"}",
            null);
    assertEquals(201, signup.status(), signup.body());
    customer = value(signup.body(), "accessToken");
    assertEquals(403, request("GET", "/manager/products", null, customer).status());
    var customerProfile = request("GET", "/customer/me", null, customer);
    assertEquals(200, customerProfile.status(), customerProfile.body());
    assertFalse(customerProfile.body().contains("password"));
    assertEquals(200, request("POST", "/auth/customer/logout", "{}", customer).status());
    assertEquals(401, request("GET", "/customer/me", null, customer).status());
  }

  @Test
  void stockAndFinancialLifecycleIsIdempotent() throws Exception {
    manager = managerLogin();
    String suffix = String.valueOf(System.nanoTime());
    var brand = request("POST", "/manager/brands", "{\"name\":\"Marca " + suffix + "\"}", manager);
    assertEquals(201, brand.status(), brand.body());
    String brandId = value(brand.body(), "id");
    var categoryResponse =
        request("POST", "/manager/categories", "{\"name\":\"Categoria " + suffix + "\"}", manager);
    assertEquals(201, categoryResponse.status(), categoryResponse.body());
    String categoryId = value(categoryResponse.body(), "id");
    var product =
        request(
            "POST",
            "/manager/products",
            "{\"name\":\"Produto\",\"description\":\"Teste\",\"price\":100,\"cost\":50,\"stock\":10,\"minimumStock\":2,\"brandId\":\""
                + brandId
                + "\",\"categoryId\":\""
                + categoryId
                + "\",\"active\":true}",
            manager);
    assertEquals(201, product.status(), product.body());
    String productId = value(product.body(), "id");
    var client =
        request(
            "POST",
            "/manager/customers",
            "{\"name\":\"Cliente\",\"phone\":\"219" + suffix.substring(suffix.length() - 8) + "\"}",
            manager);
    assertEquals(201, client.status(), client.body());
    String customerId = value(client.body(), "id");
    var order =
        request(
            "POST",
            "/manager/orders",
            "{\"customerId\":\""
                + customerId
                + "\",\"paymentMethod\":\"PIX\",\"installmentCount\":1,\"delivery\":false,\"items\":[{\"productId\":\""
                + productId
                + "\",\"quantity\":2}]}",
            manager);
    assertEquals(201, order.status(), order.body());
    String orderId = value(order.body(), "id");
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/confirm", "{}", manager).status());
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/confirm", "{}", manager).status());
    var after = request("GET", "/manager/products/" + productId, null, manager);
    assertTrue(after.body().contains("\"stock\":8"), after.body());
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/receive", "{}", manager).status());
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/receive", "{}", manager).status());
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/cancel", "{}", manager).status());
    assertEquals(
        200, request("POST", "/manager/orders/" + orderId + "/cancel", "{}", manager).status());
    var restored = request("GET", "/manager/products/" + productId, null, manager);
    assertTrue(restored.body().contains("\"stock\":10"), restored.body());
    assertEquals(
        409, request("POST", "/manager/orders/" + orderId + "/confirm", "{}", manager).status());
  }

  @Test
  void publicCatalogDoesNotExposeCostAndRejectsInvalidPayload() throws Exception {
    var catalog = request("GET", "/catalog/products", null, null);
    assertEquals(200, catalog.status(), catalog.body());
    assertFalse(catalog.body().contains("\"cost\""));
    manager = managerLogin();
    assertEquals(
        400,
        request("POST", "/manager/products", "{\"name\":\"\",\"price\":-1}", manager).status());
  }

  @Test
  void campaignsFinanceAndRelationshipEndpoints() throws Exception {
    manager = managerLogin();
    assertEquals(200, request("GET", "/manager/coupons", null, manager).status());
    assertEquals(200, request("GET", "/manager/promotions", null, manager).status());
    assertEquals(200, request("GET", "/manager/interest-rates", null, manager).status());
    assertEquals(200, request("GET", "/manager/finance", null, manager).status());
    assertEquals(200, request("GET", "/manager/audit", null, manager).status());
    assertEquals(200, request("GET", "/manager/reports/summary", null, manager).status());
    assertEquals(200, request("GET", "/manager/birthday-benefits", null, manager).status());
    assertEquals(200, request("GET", "/manager/gifts", null, manager).status());
    assertEquals(200, request("GET", "/manager/tickets", null, manager).status());
  }
}
