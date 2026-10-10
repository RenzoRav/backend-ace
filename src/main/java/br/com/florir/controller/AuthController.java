package br.com.florir.controller;
import br.com.florir.dto.AuthDtos.*;import br.com.florir.service.AuthService;import br.com.florir.security.ActorService;import br.com.florir.exception.ApiException;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.beans.factory.annotation.Value;import jakarta.validation.Valid;import jakarta.servlet.http.HttpServletRequest;import java.time.Duration;
@RestController @RequestMapping("/api/v1/auth") public class AuthController {
 private final AuthService auth;private final ActorService actor;private final boolean secure;
 public AuthController(AuthService a,ActorService actor,@Value("$"+"{florir.cookie.secure}") boolean s){auth=a;this.actor=actor;secure=s;}
 private String role(String realm){return switch(realm){case "manager"->"MANAGER";case "customer"->"CUSTOMER";default->throw ApiException.missing();};}
 private String cookieName(String realm){return "florir_"+realm+"_refresh";}
 private String cookie(String realm,String value,long age){return ResponseCookie.from(cookieName(realm),value).httpOnly(true).secure(secure).sameSite("Strict").path("/api/v1/auth/"+realm).maxAge(Duration.ofSeconds(age)).build().toString();}
 private String raw(String realm,HttpServletRequest req){if(req.getCookies()==null)return null;for(var c:req.getCookies())if(cookieName(realm).equals(c.getName()))return c.getValue();return null;}
 private ResponseEntity<Session> result(String realm,AuthService.Result r,int status){return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE,cookie(realm,r.refreshToken(),604800)).body(r.session());}
 @PostMapping("/{realm}/login") ResponseEntity<Session> login(@PathVariable String realm,@Valid @RequestBody Login data){return result(realm,auth.login(role(realm),data),200);}
 @PostMapping("/customer/register") ResponseEntity<Session> register(@Valid @RequestBody Register data){return result("customer",auth.register(data),201);}
 @PostMapping("/{realm}/refresh") ResponseEntity<Session> refresh(@PathVariable String realm,HttpServletRequest req){return result(realm,auth.refresh(role(realm),raw(realm,req)),200);}
 @PostMapping("/{realm}/logout") ResponseEntity<Void> logout(@PathVariable String realm,HttpServletRequest req){auth.logout(role(realm),raw(realm,req));return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(realm,"",0)).build();}
 @PostMapping("/{realm}/password") ResponseEntity<Void> password(@PathVariable String realm,@Valid @RequestBody PasswordChange data){if(!role(realm).equals(actor.role()))throw new ApiException(403,"Acesso negado");auth.changePassword(role(realm),actor.accountId(),data);return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie(realm,"",0)).build();}
}
