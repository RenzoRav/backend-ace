package br.com.loja.security;
import static org.junit.jupiter.api.Assertions.*;
import br.com.loja.dto.AuthDtos;
import br.com.loja.exception.ApiException;
import br.com.loja.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
@SpringBootTest(properties="spring.profiles.active=test")
class SessionFamilyIntegrationTest {
 @Autowired AuthService authService;
 @Autowired JdbcTemplate jdbcTemplate;
 AuthService.Result login(){return authService.login("MANAGER",new AuthDtos.Login("gestora@loja.local","LojaDev123!"));}
 @Test void replayRevokesSuccessorAndKeepsOtherLoginValid(){
  var first=login();var independent=login();var successor=authService.refresh("MANAGER",first.refreshToken());
  assertThrows(ApiException.class,()->authService.refresh("MANAGER",first.refreshToken()));
  assertThrows(ApiException.class,()->authService.refresh("MANAGER",successor.refreshToken()));
  assertDoesNotThrow(()->authService.refresh("MANAGER",independent.refreshToken()));
 }
 @Test void absoluteExpirationRejectsRefresh(){
  var first=login();jdbcTemplate.update("update session_family set absolute_expires_at=now()-interval '1 second' where id=(select family_id from refresh_session where token_hash=?)",AuthService.hash(first.refreshToken()));
  assertThrows(ApiException.class,()->authService.refresh("MANAGER",first.refreshToken()));
 }
}
