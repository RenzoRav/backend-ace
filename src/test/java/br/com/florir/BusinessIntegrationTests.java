package br.com.florir;

import static org.junit.jupiter.api.Assertions.*;

import br.com.florir.repository.FinancialMovementRepository;
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
  @Autowired FinancialMovementRepository finance;
  @Autowired br.com.florir.repository.CustomerAccountRepository accounts;
  final HttpClient http =
      HttpClient.newBuilder()
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .build();
  final JsonMapper mapper = JsonMapper.builder().build();

  record Reply(int status, JsonNode json, String raw) {}

  Reply call(String method, String route, Object body, String token, String key) throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + route))
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000");
    if (token != null) b.header("Authorization", "Bearer " + token);
    if (key != null) b.header("Idempotency-Key", key);
    var r =
        http.send(
            b.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    return new Reply(
        r.statusCode(),
        r.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(r.body()),
        r.body());
  }

  Reply call(String method, String route, Object body, String token) throws Exception {
    return call(method, route, body, token, null);
  }

  JsonNode ok(String method, String route, Object body, String token, int status) throws Exception {
    var r = call(method, route, body, token);
    assertEquals(status, r.status(), r.raw());
    return r.json();
  }

  String manager() throws Exception {
    return ok(
            "POST",
            "/auth/manager/login",
            Map.of("email", "gestora@florir.local", "password", "FlorirDev123!"),
            null,
            200)
        .get("accessToken")
        .asText();
  }

  String suffix() {
    return String.valueOf(System.nanoTime());
  }

  String phone(String s) {
    return "319" + s.substring(s.length() - 8);
  }

  String register() throws Exception {
    String s = suffix();
    return ok(
            "POST",
            "/auth/customer/register",
            Map.of(
                "name",
                "Cliente Teste",
                "email",
                s + "@test.local",
                "phone",
                phone(s),
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
    String s = suffix();
    String b =
        ok("POST", "/manager/brands", Map.of("name", "Marca " + s), manager, 201)
            .get("id")
            .asText();
    String c =
        ok("POST", "/manager/categories", Map.of("name", "Categoria " + s), manager, 201)
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
                b,
                "categoryId",
                c,
                "active",
                true),
            manager,
            201)
        .get("id")
        .asText();
  }

  String customer(String manager) throws Exception {
    String s = suffix();
    return ok(
            "POST",
            "/manager/customers",
            Map.of("name", "Cliente", "phone", phone(s), "birthday", "1990-10-10"),
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
    String m = manager(), p = product(m, 10, "200"), token = register();
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
            List.of(p)),
        m,
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
            List.of(p)),
        m,
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
        m,
        201);
    ok("PUT", "/manager/interest-rates", Map.of("installmentCount", 3, "percent", 5), m, 200);
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
    var data = order("999999", p, 1, "INSTALLMENTS", 3);
    data.put("delivery", true);
    data.put("addressId", address);
    data.put("couponCode", code);
    var created = ok("POST", "/customer/orders", data, token, 201);
    String id = created.get("id").asText();
    assertEquals(owner, created.get("customerId").asText());
    assertEquals(new BigDecimal("182.70"), created.get("total").decimalValue().setScale(2));
    assertEquals(0, created.get("installments").size());
    assertEquals(403, call("POST", "/manager/orders/" + id + "/confirm", Map.of(), token).status());
    var confirmed = ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), m, 200);
    assertEquals(3, confirmed.get("installments").size());
    assertEquals(
        3,
        ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), m, 200)
            .get("installments")
            .size());
    assertEquals(0, finance.findByOrderId(Long.valueOf(id)).size());
    String installment = confirmed.get("installments").get(0).get("id").asText();
    assertEquals(
        new BigDecimal("60.90"),
        confirmed.get("installments").get(0).get("amount").decimalValue().setScale(2));
    long billings = ok("GET", "/manager/billings", null, m, 200).get("totalElements").asLong();
    assertFalse(
        ok("GET", "/manager/installments/" + installment + "/whatsapp-preview", null, m, 200)
            .get("sent")
            .asBoolean());
    assertEquals(
        billings, ok("GET", "/manager/billings", null, m, 200).get("totalElements").asLong());
    ok(
        "POST",
        "/manager/installments/" + installment + "/declare-sent",
        Map.of("message", "Lembrete declarado pela gestora"),
        m,
        201);
    assertEquals(
        billings + 1, ok("GET", "/manager/billings", null, m, 200).get("totalElements").asLong());
    ok("POST", "/manager/installments/" + installment + "/receive", Map.of(), m, 200);
    ok("POST", "/manager/installments/" + installment + "/receive", Map.of(), m, 200);
    assertEquals(1, finance.findByOrderId(Long.valueOf(id)).size());
    var product = ok("GET", "/manager/products/" + p, null, m, 200);
    ok(
        "PUT",
        "/manager/products/" + p,
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
        m,
        200);
    var snapshot = ok("GET", "/customer/orders/" + id, null, token, 200);
    assertEquals("Produto Original", snapshot.get("items").get(0).get("productName").asText());
    assertEquals(new BigDecimal("182.70"), snapshot.get("total").decimalValue().setScale(2));
    assertTrue(snapshot.get("addressSnapshot").asText().contains("Rua Original"));
    var cancelled = ok("POST", "/manager/orders/" + id + "/cancel", Map.of(), m, 200);
    assertEquals("REFUNDED", cancelled.get("paymentStatus").asText());
    assertEquals("PAID", cancelled.get("installments").get(0).get("status").asText());
    assertEquals("CANCELLED", cancelled.get("installments").get(1).get("status").asText());
    ok("POST", "/manager/orders/" + id + "/cancel", Map.of(), m, 200);
    var ledger = finance.findByOrderId(Long.valueOf(id));
    assertEquals(2, ledger.size());
    assertEquals(new BigDecimal("60.90"), ledger.get(0).getAmount());
    assertEquals(new BigDecimal("60.90"), ledger.get(1).getAmount());
    assertEquals(10, ok("GET", "/manager/products/" + p, null, m, 200).get("stock").asInt());
    ok("PUT", "/manager/interest-rates", Map.of("installmentCount", 3, "percent", 0), m, 200);
  }

  @Test
  void concurrentConfirmationsNeverOversell() throws Exception {
    String m = manager(), p = product(m, 1, "100"), c = customer(m);
    String a = ok("POST", "/manager/orders", order(c, p, 1, "PIX", 1), m, 201).get("id").asText();
    String b = ok("POST", "/manager/orders", order(c, p, 1, "PIX", 1), m, 201).get("id").asText();
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var f1 =
          pool.submit(
              () -> {
                gate.await();
                return call("POST", "/manager/orders/" + a + "/confirm", Map.of(), m).status();
              });
      var f2 =
          pool.submit(
              () -> {
                gate.await();
                return call("POST", "/manager/orders/" + b + "/confirm", Map.of(), m).status();
              });
      gate.countDown();
      assertEquals(Set.of(200, 409), Set.of(f1.get(), f2.get()));
    }
    assertEquals(0, ok("GET", "/manager/products/" + p, null, m, 200).get("stock").asInt());
  }

  @Test
  void checkoutIdempotencyAndOwnership() throws Exception {
    String m = manager(), p = product(m, 10, "100"), token = register();
    var data = order(null, p, 1, "PIX", 1);
    String key = suffix();
    var first = call("POST", "/customer/orders", data, token, key);
    assertEquals(201, first.status(), first.raw());
    var retry = call("POST", "/customer/orders", data, token, key);
    assertEquals(first.json().get("id"), retry.json().get("id"));
    assertEquals(201, retry.status());
    data.put("items", List.of(Map.of("productId", p, "quantity", 2)));
    assertEquals(409, call("POST", "/customer/orders", data, token, key).status());
    String other = register();
    String id = first.json().get("id").asText();
    assertEquals(404, call("GET", "/customer/orders/" + id, null, other).status());
    assertEquals(404, call("POST", "/customer/orders/" + id + "/cancel", Map.of(), other).status());
  }

  @Test
  void benefitUniquenessAndCustomerSupportOwnership() throws Exception {
    String m = manager(), c = customer(m), token = register();
    var benefit = Map.of("customerId", c, "year", 2026);
    ok("POST", "/manager/birthday-benefits", benefit, m, 201);
    assertEquals(409, call("POST", "/manager/birthday-benefits", benefit, m).status());
    ok("POST", "/manager/birthday-benefits", Map.of("customerId", c, "year", 2027), m, 201);
    ok(
        "POST",
        "/manager/gifts",
        Map.of("customerId", c, "description", "Brinde de aniversário", "date", "2026-10-10"),
        m,
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
        m,
        200);
    assertEquals(
        "RESOLVED", ok("GET", "/customer/tickets", null, token, 200).get(0).get("status").asText());
  }

  @Test
  void refreshRotationOriginProtectionAndPasswordRevocation() throws Exception {
    String m = manager();
    var refresh = ok("POST", "/auth/manager/refresh", Map.of(), null, 200);
    assertEquals("MANAGER", refresh.get("role").asText());
    assertEquals(
        200, call("GET", "/manager/products", null, refresh.get("accessToken").asText()).status());
    var bad =
        http.send(
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
        mapper.readTree(
            new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8));
    var account = accounts.findById(Long.valueOf(claims.get("sub").asText())).orElseThrow();
    account.setCredentialVersion(account.getCredentialVersion() + 1);
    accounts.saveAndFlush(account);
    assertEquals(401, call("POST", "/auth/customer/refresh", Map.of(), null).status());
  }

  @Test
  void oldLogoutCookieDoesNotRevokeNewLogin() throws Exception {
    manager();
    var cookies =
        ((CookieManager) http.cookieHandler().orElseThrow()).getCookieStore().getCookies();
    String old =
        cookies.stream()
            .filter(c -> c.getName().equals("florir_manager_refresh"))
            .findFirst()
            .orElseThrow()
            .getValue();
    ok("POST", "/auth/manager/logout", Map.of(), null, 200);
    String fresh = manager();
    var req =
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1/auth/manager/logout"))
            .header("Origin", "http://localhost:3000")
            .header("Cookie", "florir_manager_refresh=" + old)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();
    assertEquals(
        200,
        HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
    assertEquals(200, call("GET", "/manager/products", null, fresh).status());
  }

  @Test
  void zeroTotalInstallmentOrderIsAutomaticallySettled() throws Exception {
    String m = manager(), p = product(m, 2, "0"), c = customer(m);
    String id =
        ok("POST", "/manager/orders", order(c, p, 1, "INSTALLMENTS", 3), m, 201).get("id").asText();
    var result = ok("POST", "/manager/orders/" + id + "/confirm", Map.of(), m, 200);
    assertEquals("APPROVED", result.get("paymentStatus").asText());
    assertEquals(0, result.get("installments").size());
    assertEquals(0, finance.findByOrderId(Long.valueOf(id)).size());
  }

  @Test
  void diagramSearchDetailsInstallmentsAlertsAndBirthdayPreview() throws Exception {
    String m = manager(), p = product(m, 1, "100"), c = customer(m);
    var detail = ok("GET", "/catalog/products/" + p, null, null, 200);
    assertFalse(detail.has("cost"));
    assertEquals(
        0,
        ok("GET", "/catalog/products?search=NoMatch" + suffix(), null, null, 200)
            .get("totalElements")
            .asLong());
    String brand =
        ok("GET", "/manager/products/" + p, null, m, 200).get("brand").get("id").asText();
    assertEquals(
        1,
        ok("GET", "/catalog/products?brandId=" + brand, null, null, 200)
            .get("totalElements")
            .asLong());
    ok("GET", "/manager/installments", null, m, 200);
    ok("GET", "/manager/alerts", null, m, 200);
    ok("GET", "/manager/alerts/low-stock", null, m, 200);
    ok("GET", "/manager/alerts/overdue", null, m, 200);
    assertFalse(
        ok("GET", "/manager/customers/" + c + "/birthday-whatsapp-preview", null, m, 200)
            .get("sent")
            .asBoolean());
  }

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void productPromotionUsesDiagramCompositePrimaryKey() {
    var columns =
        jdbc.queryForList(
            "select cu.column_name from information_schema.table_constraints c join"
                + " information_schema.constraint_column_usage cu on"
                + " c.constraint_name=cu.constraint_name and c.table_schema=cu.table_schema where"
                + " c.table_schema='public' and c.table_name='product_promotion' and"
                + " c.constraint_type='PRIMARY KEY'",
            String.class);
    assertEquals(Set.of("product_id", "promotion_id"), new HashSet<>(columns));
  }
}
