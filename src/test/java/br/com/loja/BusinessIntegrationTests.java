package br.com.loja;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.repository.FinancialMovementRepository;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"})
class BusinessIntegrationTests {
  @LocalServerPort int port;
  @Autowired FinancialMovementRepository financialMovementRepository;
  @Autowired br.com.loja.repository.CustomerAccountRepository customerAccountRepository;
  final HttpClient httpClient =
      HttpClient.newBuilder()
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .build();
  final JsonMapper jsonMapper = JsonMapper.builder().build();

  record Reply(int status, JsonNode json, String raw) {}

  Reply call(String method, String route, Object body, String token, String idempotencyKey)
      throws Exception {
    var requestBuilder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + route))
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000");
    if (token != null) requestBuilder.header("Authorization", "Bearer " + token);
    if (idempotencyKey != null) requestBuilder.header("Idempotency-Key", idempotencyKey);
    var response =
        httpClient.send(
            requestBuilder
                .method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return new Reply(
        response.statusCode(),
        response.body().isBlank()
            ? jsonMapper.createObjectNode()
            : jsonMapper.readTree(response.body()),
        response.body());
  }

  Reply call(String method, String route, Object body, String token) throws Exception {
    return call(method, route, body, token, null);
  }

  JsonNode ok(String method, String route, Object body, String token, int status) throws Exception {
    var response = call(method, route, body, token);
    assertEquals(status, response.status(), response.raw());
    return response.json();
  }

  String manager() throws Exception {
    return ok(
            "POST",
            "/auth/manager/login",
            Map.of("email", "gestora@loja.local", "password", "LojaDev123!"),
            null,
            200)
        .get("accessToken")
        .asText();
  }

  String suffix() {
    return String.valueOf(System.nanoTime());
  }

  String phone(String uniqueSuffix) {
    return "319" + uniqueSuffix.substring(uniqueSuffix.length() - 8);
  }

  String register() throws Exception {
    String uniqueSuffix = suffix();
    return ok(
            "POST",
            "/auth/customer/register",
            Map.of(
                "name",
                "Cliente Teste",
                "email",
                uniqueSuffix + "@test.local",
                "phone",
                phone(uniqueSuffix),
                "password",
                "TestPass123!",
                "birthday",
                "1990-10-10"),
            null,
            201)
        .get("accessToken")
        .asText();
  }

  String product(String manager, int stock, String price) throws Exception {
    String uniqueSuffix = suffix();
    String brandId =
        ok("POST", "/manager/brands", Map.of("name", "Marca " + uniqueSuffix), manager, 201)
            .get("id")
            .asText();
    String categoryId =
        ok("POST", "/manager/categories", Map.of("name", "Categoria " + uniqueSuffix), manager, 201)
            .get("id")
            .asText();
    return ok(
            "POST",
            "/manager/products",
            Map.of(
                "name",
                "Produto Original",
                "price",
                new BigDecimal(price),
                "cost",
                50,
                "stock",
                stock,
                "minimumStock",
                1,
                "brandId",
                brandId,
                "categoryId",
                categoryId,
                "active",
                true),
            manager,
            201)
        .get("id")
        .asText();
  }

  String customer(String manager) throws Exception {
    String uniqueSuffix = suffix();
    return ok(
            "POST",
            "/manager/customers",
            Map.of("name", "Cliente", "phone", phone(uniqueSuffix), "birthday", "1990-10-10"),
            manager,
            201)
        .get("id")
        .asText();
  }

  Map<String, Object> order(
      String customer, String product, int quantity, String method, int count) {
    var data = new HashMap<String, Object>();
    if (customer != null) data.put("customerId", customer);
    data.put("paymentMethod", method);
    data.put("installmentCount", count);
    data.put("delivery", false);
    data.put("items", List.of(Map.of("productId", product, "quantity", quantity)));
    return data;
  }

  @Test
  void promotionCouponShippingInterestSnapshotsAndPartialRefund() throws Exception {
    String managerAccessToken = manager(),
        productId = product(managerAccessToken, 10, "200"),
        token = register();
    String owner = ok("GET", "/customer/me", null, token, 200).get("id").asText();
    String code = "TEST" + suffix();
    ok(
        "POST",
        "/manager/promotions",
        Map.of(
            "name",
            "Promoção",
            "percent",
            10,
            "validFrom",
            "2020-01-01",
            "validUntil",
            "2030-12-31",
            "productIds",
            List.of(productId)),
        managerAccessToken,
        201);
    ok(
        "POST",
        "/manager/promotions",
        Map.of(
            "name",
            "Promoção menor",
            "percent",
            5,
            "validFrom",
            "2020-01-01",
            "validUntil",
            "2030-12-31",
            "productIds",
            List.of(productId)),
        managerAccessToken,
        201);
    ok(
        "POST",
        "/manager/coupons",
        Map.of(
            "code",
            code,
            "type",
            "PERCENT",
            "value",
            10,
            "validFrom",
            "2020-01-01",
            "validUntil",
            "2030-12-31",
            "usageLimit",
            1),
        managerAccessToken,
        201);
    ok(
        "PUT",
        "/manager/interest-rates",
        Map.of("installmentCount", 3, "percent", 5),
        managerAccessToken,
        200);
    String address =
        ok(
                "POST",
                "/customer/addresses",
                Map.of(
                    "label",
                    "Casa",
                    "postalCode",
                    "01001000",
                    "street",
                    "Rua Original",
                    "number",
                    "10",
                    "district",
                    "Centro",
                    "city",
                    "São Paulo",
                    "state",
                    "SP"),
                token,
                201)
            .get("id")
            .asText();
    var data = order("999999", productId, 1, "INSTALLMENTS", 3);
    data.put("delivery", true);
    data.put("addressId", address);
    data.put("couponCode", code);
    var created = ok("POST", "/customer/orders", data, token, 201);
    String id = created.get("id").asText();
    assertEquals(owner, created.get("customerId").asText());
    assertEquals(new BigDecimal("182.70"), created.get("total").decimalValue().setScale(2));
    assertEquals(0, created.get("installments").size());
    assertEquals(403, call("POST", "/manager/orders/" + id + "/confirm", Map.of(), token).status());
    var confirmed =
        ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), managerAccessToken, 200);
    assertEquals(3, confirmed.get("installments").size());
    assertEquals(
        3,
        ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), managerAccessToken, 200)
            .get("installments")
            .size());
    assertEquals(0, financialMovementRepository.findByOrderId(Long.valueOf(id)).size());
    String installment = confirmed.get("installments").get(0).get("id").asText();
    assertEquals(
        new BigDecimal("60.90"),
        confirmed.get("installments").get(0).get("amount").decimalValue().setScale(2));
    long billings =
        ok("GET", "/manager/billings", null, managerAccessToken, 200).get("totalElements").asLong();
    assertFalse(
        ok(
                "GET",
                "/manager/installments/" + installment + "/whatsapp-preview",
                null,
                managerAccessToken,
                200)
            .get("sent")
            .asBoolean());
    assertEquals(
        billings,
        ok("GET", "/manager/billings", null, managerAccessToken, 200)
            .get("totalElements")
            .asLong());
    ok(
        "POST",
        "/manager/installments/" + installment + "/declare-sent",
        Map.of("message", "Lembrete declarado pela gestora"),
        managerAccessToken,
        201);
    assertEquals(
        billings + 1,
        ok("GET", "/manager/billings", null, managerAccessToken, 200)
            .get("totalElements")
            .asLong());
    ok(
        "POST",
        "/manager/installments/" + installment + "/receive",
        Map.of(),
        managerAccessToken,
        200);
    ok(
        "POST",
        "/manager/installments/" + installment + "/receive",
        Map.of(),
        managerAccessToken,
        200);
    assertEquals(1, financialMovementRepository.findByOrderId(Long.valueOf(id)).size());
    var product = ok("GET", "/manager/products/" + productId, null, managerAccessToken, 200);
    ok(
        "PUT",
        "/manager/products/" + productId,
        Map.of(
            "name",
            "Nome Alterado",
            "price",
            999,
            "cost",
            90,
            "stock",
            9,
            "minimumStock",
            1,
            "brandId",
            product.get("brand").get("id").asText(),
            "categoryId",
            product.get("category").get("id").asText(),
            "active",
            true),
        managerAccessToken,
        200);
    var snapshot = ok("GET", "/customer/orders/" + id, null, token, 200);
    assertEquals("Produto Original", snapshot.get("items").get(0).get("productName").asText());
    assertEquals(new BigDecimal("182.70"), snapshot.get("total").decimalValue().setScale(2));
    assertTrue(snapshot.get("addressSnapshot").asText().contains("Rua Original"));
    var cancelled =
        ok("POST", "/manager/orders/" + id + "/cancel", Map.of(), managerAccessToken, 200);
    assertEquals("REFUNDED", cancelled.get("paymentStatus").asText());
    assertEquals("PAID", cancelled.get("installments").get(0).get("status").asText());
    assertEquals("CANCELLED", cancelled.get("installments").get(1).get("status").asText());
    ok("POST", "/manager/orders/" + id + "/cancel", Map.of(), managerAccessToken, 200);
    var ledger = financialMovementRepository.findByOrderId(Long.valueOf(id));
    assertEquals(2, ledger.size());
    assertEquals(new BigDecimal("60.90"), ledger.get(0).getAmount());
    assertEquals(new BigDecimal("60.90"), ledger.get(1).getAmount());
    assertEquals(
        10,
        ok("GET", "/manager/products/" + productId, null, managerAccessToken, 200)
            .get("stock")
            .asInt());
    ok(
        "PUT",
        "/manager/interest-rates",
        Map.of("installmentCount", 3, "percent", 0),
        managerAccessToken,
        200);
  }

  @Test
  void concurrentConfirmationsNeverOversell() throws Exception {
    String managerAccessToken = manager(),
        productId = product(managerAccessToken, 1, "100"),
        customerId = customer(managerAccessToken);
    String firstOrderId =
        ok(
                "POST",
                "/manager/orders",
                order(customerId, productId, 1, "PIX", 1),
                managerAccessToken,
                201)
            .get("id")
            .asText();
    String secondOrderId =
        ok(
                "POST",
                "/manager/orders",
                order(customerId, productId, 1, "PIX", 1),
                managerAccessToken,
                201)
            .get("id")
            .asText();
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var firstConfirmation =
          pool.submit(
              () -> {
                gate.await();
                return call(
                        "POST",
                        "/manager/orders/" + firstOrderId + "/confirm",
                        Map.of(),
                        managerAccessToken)
                    .status();
              });
      var secondConfirmation =
          pool.submit(
              () -> {
                gate.await();
                return call(
                        "POST",
                        "/manager/orders/" + secondOrderId + "/confirm",
                        Map.of(),
                        managerAccessToken)
                    .status();
              });
      gate.countDown();
      assertEquals(Set.of(200, 409), Set.of(firstConfirmation.get(), secondConfirmation.get()));
    }
    assertEquals(
        0,
        ok("GET", "/manager/products/" + productId, null, managerAccessToken, 200)
            .get("stock")
            .asInt());
  }

  @Test
  void checkoutIdempotencyAndOwnership() throws Exception {
    String managerAccessToken = manager(),
        productId = product(managerAccessToken, 10, "100"),
        token = register();
    var data = order(null, productId, 1, "PIX", 1);
    String idempotencyKey = suffix();
    var first = call("POST", "/customer/orders", data, token, idempotencyKey);
    assertEquals(201, first.status(), first.raw());
    var retry = call("POST", "/customer/orders", data, token, idempotencyKey);
    assertEquals(first.json().get("id"), retry.json().get("id"));
    assertEquals(201, retry.status());
    data.put("items", List.of(Map.of("productId", productId, "quantity", 2)));
    assertEquals(409, call("POST", "/customer/orders", data, token, idempotencyKey).status());
    String other = register();
    String id = first.json().get("id").asText();
    assertEquals(404, call("GET", "/customer/orders/" + id, null, other).status());
    assertEquals(404, call("POST", "/customer/orders/" + id + "/cancel", Map.of(), other).status());
  }

  @Test
  void benefitUniquenessAndCustomerSupportOwnership() throws Exception {
    String managerAccessToken = manager(),
        customerId = customer(managerAccessToken),
        token = register();
    var benefit = Map.of("customerId", customerId, "year", 2026);
    ok("POST", "/manager/birthday-benefits", benefit, managerAccessToken, 201);
    assertEquals(
        409, call("POST", "/manager/birthday-benefits", benefit, managerAccessToken).status());
    ok(
        "POST",
        "/manager/birthday-benefits",
        Map.of("customerId", customerId, "year", 2027),
        managerAccessToken,
        201);
    ok(
        "POST",
        "/manager/gifts",
        Map.of(
            "customerId", customerId, "description", "Brinde de aniversário", "date", "2026-10-10"),
        managerAccessToken,
        201);
    String ticket =
        ok(
                "POST",
                "/customer/tickets",
                Map.of("subject", "Ajuda", "message", "Preciso de atendimento"),
                token,
                201)
            .get("id")
            .asText();
    assertEquals(
        403,
        call(
                "PUT",
                "/manager/tickets/" + ticket + "/reply",
                Map.of("reply", "Resolvido", "status", "RESOLVED"),
                token)
            .status());
    ok(
        "PUT",
        "/manager/tickets/" + ticket + "/reply",
        Map.of("reply", "Resolvido", "status", "RESOLVED"),
        managerAccessToken,
        200);
    assertEquals(
        "RESOLVED", ok("GET", "/customer/tickets", null, token, 200).get(0).get("status").asText());
  }

  @Test
  void refreshRotationOriginProtectionAndPasswordRevocation() throws Exception {
    String managerAccessToken = manager();
    var refresh = ok("POST", "/auth/manager/refresh", Map.of(), null, 200);
    assertEquals("MANAGER", refresh.get("role").asText());
    assertEquals(
        200, call("GET", "/manager/products", null, refresh.get("accessToken").asText()).status());
    var bad =
        httpClient.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/api/v1/auth/manager/refresh"))
                .header("Origin", "https://evil.example")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(403, bad.statusCode());
    String token = register();
    ok(
        "POST",
        "/auth/customer/password",
        Map.of("currentPassword", "TestPass123!", "newPassword", "NewTestPass123!"),
        token,
        204);
    assertEquals(401, call("GET", "/customer/me", null, token).status());
    assertEquals(401, call("POST", "/auth/customer/refresh", Map.of(), null).status());
  }

  @Test
  void staleRefreshVersionCannotReauthenticate() throws Exception {
    String token = register();
    var claims =
        jsonMapper.readTree(
            new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8));
    var account =
        customerAccountRepository.findById(Long.valueOf(claims.get("sub").asText())).orElseThrow();
    account.setCredentialVersion(account.getCredentialVersion() + 1);
    customerAccountRepository.saveAndFlush(account);
    assertEquals(401, call("POST", "/auth/customer/refresh", Map.of(), null).status());
  }

  @Test
  void oldLogoutCookieDoesNotRevokeNewLogin() throws Exception {
    manager();
    var cookies =
        ((CookieManager) httpClient.cookieHandler().orElseThrow()).getCookieStore().getCookies();
    String old =
        cookies.stream()
            .filter(cookie -> cookie.getName().equals("loja_manager_refresh"))
            .findFirst()
            .orElseThrow()
            .getValue();
    ok("POST", "/auth/manager/logout", Map.of(), null, 200);
    String fresh = manager();
    var requestBuilder =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1/auth/manager/logout"))
            .header("Origin", "http://localhost:3000")
            .header("Cookie", "loja_manager_refresh=" + old)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
    assertEquals(
        200,
        HttpClient.newHttpClient()
            .send(requestBuilder, HttpResponse.BodyHandlers.ofString())
            .statusCode());
    assertEquals(200, call("GET", "/manager/products", null, fresh).status());
  }

  @Test
  void zeroTotalInstallmentOrderIsAutomaticallySettled() throws Exception {
    String managerAccessToken = manager(),
        productId = product(managerAccessToken, 2, "0"),
        customerId = customer(managerAccessToken);
    String id =
        ok(
                "POST",
                "/manager/orders",
                order(customerId, productId, 1, "INSTALLMENTS", 3),
                managerAccessToken,
                201)
            .get("id")
            .asText();
    var result =
        ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), managerAccessToken, 200);
    assertEquals("APPROVED", result.get("paymentStatus").asText());
    assertEquals(0, result.get("installments").size());
    assertEquals(0, financialMovementRepository.findByOrderId(Long.valueOf(id)).size());
  }

  @Test
  void diagramSearchDetailsInstallmentsAlertsAndBirthdayPreview() throws Exception {
    String managerAccessToken = manager(),
        productId = product(managerAccessToken, 1, "100"),
        customerId = customer(managerAccessToken);
    var detail = ok("GET", "/catalog/products/" + productId, null, null, 200);
    assertFalse(detail.has("cost"));
    assertEquals(
        0,
        ok("GET", "/catalog/products?search=NoMatch" + suffix(), null, null, 200)
            .get("totalElements")
            .asLong());
    String brand =
        ok("GET", "/manager/products/" + productId, null, managerAccessToken, 200)
            .get("brand")
            .get("id")
            .asText();
    assertEquals(
        1,
        ok("GET", "/catalog/products?brandId=" + brand, null, null, 200)
            .get("totalElements")
            .asLong());
    ok("GET", "/manager/installments", null, managerAccessToken, 200);
    ok("GET", "/manager/alerts", null, managerAccessToken, 200);
    ok("GET", "/manager/alerts/low-stock", null, managerAccessToken, 200);
    ok("GET", "/manager/alerts/overdue", null, managerAccessToken, 200);
    assertFalse(
        ok(
                "GET",
                "/manager/customers/" + customerId + "/birthday-whatsapp-preview",
                null,
                managerAccessToken,
                200)
            .get("sent")
            .asBoolean());
  }

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  @Test
  void productPromotionUsesDiagramCompositePrimaryKey() {
    var columns =
        jdbcTemplate.queryForList(
            "select cu.column_name from information_schema.table_constraints c join"
                + " information_schema.constraint_column_usage cu on"
                + " c.constraint_name=cu.constraint_name and c.table_schema=cu.table_schema where"
                + " c.table_schema='public' and c.table_name='product_promotion' and"
                + " c.constraint_type='PRIMARY KEY'",
            String.class);
    assertEquals(Set.of("product_id", "promotion_id"), new HashSet<>(columns));
  }
}
