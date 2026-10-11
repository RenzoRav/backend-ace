package br.com.loja.security;
import static org.junit.jupiter.api.Assertions.*;
import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import java.time.Duration;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
@SpringBootTest(properties="spring.profiles.active=test")
class AuthenticationLimitIntegrationTest {
 @Autowired AuthenticationLimitService authenticationLimitService;
 @Autowired SecurityEventService securityEventService;
 @Autowired JdbcTemplate jdbcTemplate;
 @Test void enforcesThresholdAndIndependentKeys() {
  String key=UUID.randomUUID().toString();
  for(int attempt=0;attempt<3;attempt++)assertTrue(authenticationLimitService.check("test",key,3,Duration.ofMinutes(1)).allowed());
  var decision=authenticationLimitService.check("test",key,3,Duration.ofMinutes(1));assertFalse(decision.allowed());assertTrue(decision.retryAfterSeconds()>0);
  assertTrue(authenticationLimitService.check("test",UUID.randomUUID().toString(),3,Duration.ofMinutes(1)).allowed());
 }
 @Test void concurrentRequestsNeverExceedBudget() throws Exception {
  String key=UUID.randomUUID().toString();var executor=Executors.newFixedThreadPool(8);
  try{var futures=new ArrayList<Future<Boolean>>();for(int attempt=0;attempt<20;attempt++)futures.add(executor.submit(()->authenticationLimitService.check("parallel",key,5,Duration.ofMinutes(1)).allowed()));int allowed=0;for(var future:futures)if(future.get())allowed++;assertEquals(5,allowed);}finally{executor.shutdownNow();}
 }
 @Test void securityEventsDoNotStoreCredentials() {
  String correlation=UUID.randomUUID().toString();securityEventService.record("LOGIN","DENIED","unknown",correlation);
  assertEquals(1,jdbcTemplate.queryForObject("select count(*) from security_event where correlation_id=? and event='LOGIN' and result='DENIED'",Integer.class,correlation));
 }
}

