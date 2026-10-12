package br.com.loja.security;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.service.AuthService;
import java.net.URI;
import java.net.http.*;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=test")
class ResponsePaginationIntegrationTest {
  @LocalServerPort int port;
  @Autowired AuthService authService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void offersPagedListsAndRejectsInvalidPageSizes() throws Exception {
    var session =
        authService.register(
            new AuthDtos.Register(
                "Pagination",
                "page-" + System.nanoTime() + "@example.invalid",
                "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999),
                "PaginationPass123!",
                null));
    var client = HttpClient.newHttpClient();
    for (String resource : new String[] {"orders", "addresses", "tickets"}) {
      var good =
          client.send(
              HttpRequest.newBuilder(
                      URI.create(
                          "http://localhost:"
                              + port
                              + "/api/v1/customer/"
                              + resource
                              + "/paged?page=0&size=20"))
                  .header("Authorization", "Bearer " + session.session().accessToken())
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, good.statusCode());
      assertTrue(good.body().contains("totalElements"));
      var bad =
          client.send(
              HttpRequest.newBuilder(
                      URI.create(
                          "http://localhost:"
                              + port
                              + "/api/v1/customer/"
                              + resource
                              + "/paged?page=-1&size=101"))
                  .header("Authorization", "Bearer " + session.session().accessToken())
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(400, bad.statusCode());
    }
  }

  AuthService.Result register() {
    return authService.register(
        new AuthDtos.Register(
            "Pages",
            "pages-" + System.nanoTime() + "@example.invalid",
            "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999),
            "PaginationPass123!",
            null));
  }

  HttpResponse<String> get(String path, AuthService.Result session) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("Authorization", "Bearer " + session.session().accessToken())
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void pagesAreCompleteAndBelongToTheAuthenticatedCustomer() throws Exception {
    var first = register();
    var second = register();
    long customer =
        jdbcTemplate.queryForObject(
            "select customer_id from customer_account where id=?",
            Long.class,
            Long.valueOf(first.session().accountId()));
    for (int index = 0; index < 3; index++)
      jdbcTemplate.update(
          "insert into"
              + " customer_address(customer_id,label,postal_code,street,number,district,city,state)"
              + " values(?,?,'12345678','Street','1','District','City','SP')",
          customer,
          "address-" + index);
    var mapper = JsonMapper.builder().build();
    var identifiers = new HashSet<String>();
    for (int page = 0; page < 2; page++) {
      var response = get("/customer/addresses/paged?page=" + page + "&size=2", first);
      assertEquals(200, response.statusCode());
      var data = mapper.readTree(response.body());
      assertEquals(3, data.get("totalElements").asInt());
      for (var address : data.get("content"))
        assertTrue(identifiers.add(address.get("id").asText()));
    }
    assertEquals(3, identifiers.size());
    assertEquals(
        0,
        mapper
            .readTree(get("/customer/addresses/paged", second).body())
            .get("totalElements")
            .asInt());
  }

  @Test
  void legacyListsDoNotSilentlyTruncate() throws Exception {
    var first = register();
    long customer =
        jdbcTemplate.queryForObject(
            "select customer_id from customer_account where id=?",
            Long.class,
            Long.valueOf(first.session().accountId()));
    for (int index = 0; index < 101; index++)
      jdbcTemplate.update(
          "insert into"
              + " customer_address(customer_id,label,postal_code,street,number,district,city,state)"
              + " values(?,'address','12345678','Street','1','District','City','SP')",
          customer);
    assertEquals(413, get("/customer/addresses", first).statusCode());
    assertTrue(
        get("/customer/addresses/paged?size=100", first).body().contains("\"totalElements\":101"));
  }
}
