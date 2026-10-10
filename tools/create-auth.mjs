import fs from 'node:fs';import path from 'node:path';const put=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};const java=(p,s)=>put('src/main/java/br/com/florir/'+p+'.java',s);
java('exception/ApiException',`package br.com.florir.exception;
public class ApiException extends RuntimeException { private final int status;public ApiException(int status,String message){super(message);this.status=status;}public int status(){return status;}public static ApiException missing(){return new ApiException(404,"Registro não encontrado");}public static ApiException conflict(String m){return new ApiException(409,m);} }
`);
java('exception/ApiExceptionHandler',`package br.com.florir.exception;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.dao.*;import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {
 public record Error(String message){}
 @ExceptionHandler(ApiException.class) ResponseEntity<Error> api(ApiException e){return ResponseEntity.status(e.status()).body(new Error(e.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) ResponseEntity<Error> invalid(Exception e){return ResponseEntity.badRequest().body(new Error("Dados inválidos"));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Error> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(new Error(e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).collect(java.util.stream.Collectors.joining("; "))));}
 @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class,org.springframework.dao.PessimisticLockingFailureException.class}) ResponseEntity<Error> conflict(Exception e){return ResponseEntity.status(409).body(new Error("Conflito de dados; atualize e tente novamente"));}
}
`);
java('dto/AuthDtos',`package br.com.florir.dto;
import jakarta.validation.constraints.*;import java.time.LocalDate;
public final class AuthDtos {
 public record Login(@NotBlank @Email String email,@NotBlank String password){}
 public record Register(@NotBlank @Size(max=255) String name,@NotBlank @Email String email,@NotBlank String phone,@NotBlank @Size(min=10,max=72) String password,LocalDate birthday){}
 public record Session(String accessToken,String tokenType,long expiresIn,String role,String accountId,String name){}
 public record PasswordChange(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=72) String newPassword){}
}
`);
java('security/ActorService',`package br.com.florir.security;
import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.oauth2.jwt.Jwt;
@Service public class ActorService {
 private final ManagerRepository managers;private final CustomerAccountRepository accounts;
 public ActorService(ManagerRepository m,CustomerAccountRepository a){managers=m;accounts=a;}
 public Jwt jwt(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null || !(auth.getPrincipal() instanceof Jwt j))throw new ApiException(401,"Autenticação necessária");return j;}
 public String role(){return jwt().getClaimAsString("role");}
 public Long accountId(){return Long.valueOf(jwt().getSubject());}
 public Manager manager(){if(!"MANAGER".equals(role()))throw new ApiException(403,"Acesso negado");return managers.findById(accountId()).orElseThrow(ApiException::missing);}
 public Customer customer(){if(!"CUSTOMER".equals(role()))throw new ApiException(403,"Acesso negado");return accounts.findById(accountId()).orElseThrow(ApiException::missing).getCustomer();}
 public void owns(Customer customer){if("CUSTOMER".equals(role())&&!customer.getId().equals(customer().getId()))throw new ApiException(404,"Registro não encontrado");}
}
`);
java('security/CredentialVersionFilter',`package br.com.florir.security;
import br.com.florir.repository.*;import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.oauth2.jwt.Jwt;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class CredentialVersionFilter extends OncePerRequestFilter {
 private final ManagerRepository managers;private final CustomerAccountRepository accounts;
 public CredentialVersionFilter(ManagerRepository m,CustomerAccountRepository a){managers=m;accounts=a;}
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null && auth.getPrincipal() instanceof Jwt jwt){
   boolean valid=false;try {Long id=Long.valueOf(jwt.getSubject());Long version=jwt.getClaim("credentialVersion");if("MANAGER".equals(jwt.getClaimAsString("role")))valid=managers.findById(id).map(m->m.getActive()&&m.getCredentialVersion()==version).orElse(false);else if("CUSTOMER".equals(jwt.getClaimAsString("role")))valid=accounts.findById(id).map(a->a.getCredentialVersion()==version).orElse(false);}catch(RuntimeException ignored){}
   if(!valid){SecurityContextHolder.clearContext();response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\\"message\\":\\"Sessão expirada\\"}");return;}
  }chain.doFilter(request,response);
 }
}
`);
java('security/AuthOriginFilter',`package br.com.florir.security;
import org.springframework.web.filter.OncePerRequestFilter;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class AuthOriginFilter extends OncePerRequestFilter {
 private final String origin;public AuthOriginFilter(String origin){this.origin=origin;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  if(req.getRequestURI().startsWith("/api/v1/auth/") && !req.getMethod().equals("OPTIONS") && !req.getMethod().equals("GET")) {
   String sent=req.getHeader("Origin");boolean cookie=req.getRequestURI().endsWith("/refresh")||req.getRequestURI().endsWith("/logout");
   if((sent!=null&&!origin.equals(sent))||(cookie&&sent==null)){res.setStatus(403);return;}
  }chain.doFilter(req,res);
 }
}
`);
java('config/SecurityConfig',`package br.com.florir.config;
import br.com.florir.security.*;import br.com.florir.repository.*;import org.springframework.context.annotation.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.crypto.password.*;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.oauth2.jwt.*;import org.springframework.security.oauth2.jose.jws.MacAlgorithm;import org.springframework.security.oauth2.core.*;import org.springframework.security.oauth2.server.resource.authentication.*;import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;import org.springframework.web.cors.*;import javax.crypto.spec.SecretKeySpec;import java.util.*;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean JwtEncoder encoder(@Value("$"+"{florir.jwt.secret}") String secret){if(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes");return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
 @Bean JwtDecoder decoder(@Value("$"+"{florir.jwt.secret}") String secret,@Value("$"+"{florir.jwt.issuer}") String issuer,@Value("$"+"{florir.jwt.audience}") String audience){var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();OAuth2TokenValidator<Jwt> aud=j->j.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),aud));return decoder;}
 @Bean SecurityFilterChain security(HttpSecurity http,ManagerRepository managers,CustomerAccountRepository accounts,@Value("$"+"{florir.allowed-origin}") String origin)throws Exception {
  var authorities=new JwtGrantedAuthoritiesConverter();authorities.setAuthoritiesClaimName("role");authorities.setAuthorityPrefix("ROLE_");var converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(authorities);
  http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/**","/api/v1/catalog/**","/api/v1/health").permitAll().requestMatchers("/api/v1/manager/**").hasRole("MANAGER").requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER").anyRequest().authenticated())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter)))
   .addFilterBefore(new AuthOriginFilter(origin),BearerTokenAuthenticationFilter.class)
   .addFilterAfter(new CredentialVersionFilter(managers,accounts),BearerTokenAuthenticationFilter.class);
  return http.build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("$"+"{florir.allowed-origin}") String origin){var c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","Authorization","Idempotency-Key"));c.setAllowCredentials(true);var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;}
}
`);
java('service/AuthService',`package br.com.florir.service;
import br.com.florir.dto.AuthDtos.*;import br.com.florir.model.*;import br.com.florir.repository.*;import br.com.florir.exception.ApiException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.oauth2.jwt.*;import org.springframework.security.oauth2.jose.jws.MacAlgorithm;import org.springframework.beans.factory.annotation.Value;import java.time.*;import java.util.*;import java.security.*;import java.nio.charset.StandardCharsets;
@Service @Transactional public class AuthService {
 private final ManagerRepository managers;private final CustomerAccountRepository accounts;private final CustomerRepository customers;private final RefreshSessionRepository sessions;private final PasswordEncoder passwords;private final JwtEncoder encoder;private final String issuer,audience;private final String dummyHash;
 public AuthService(ManagerRepository m,CustomerAccountRepository a,CustomerRepository c,RefreshSessionRepository s,PasswordEncoder p,JwtEncoder e,@Value("$"+"{florir.jwt.issuer}") String i,@Value("$"+"{florir.jwt.audience}") String aud){managers=m;accounts=a;customers=c;sessions=s;passwords=p;encoder=e;issuer=i;audience=aud;dummyHash=p.encode(UUID.randomUUID().toString());}
 public record Result(Session session,String refreshToken){}
 private String email(String e){return e.strip().toLowerCase(Locale.ROOT);}
 public Result login(String role,Login login){
  if("MANAGER".equals(role)){var m=managers.findByEmail(email(login.email())).orElse(null);boolean matches=passwords.matches(login.password(),m==null?dummyHash:m.getPasswordHash());if(m==null||!m.getActive()||!matches)throw new ApiException(401,"Credenciais inválidas");return issue(role,m.getId(),m.getName(),m.getCredentialVersion());}
  var a=accounts.findByEmail(email(login.email())).orElse(null);boolean matches=passwords.matches(login.password(),a==null?dummyHash:a.getPasswordHash());if(a==null||!matches)throw new ApiException(401,"Credenciais inválidas");return issue(role,a.getId(),a.getCustomer().getName(),a.getCredentialVersion());
 }
 public Result register(Register data){String phone=PhoneNumbers.normalize(data.phone());String mail=email(data.email());if(accounts.existsByEmail(mail)||customers.existsByPhone(phone))throw ApiException.conflict("E-mail ou telefone já cadastrado");var c=new Customer();c.setName(data.name().strip());c.setPhone(phone);c.setBirthday(data.birthday());customers.save(c);var a=new CustomerAccount();a.setCustomer(c);a.setEmail(mail);a.setPasswordHash(passwords.encode(data.password()));accounts.save(a);return issue("CUSTOMER",a.getId(),c.getName(),a.getCredentialVersion());}
 private Result issue(String role,Long id,String name,long version){
  Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(id.toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).id(UUID.randomUUID().toString()).claim("role",role).claim("credentialVersion",version).build();String jwt=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);var refresh=new RefreshSession();refresh.setRole(role);refresh.setAccountId(id);refresh.setTokenHash(hash(raw));refresh.setExpiresAt(now.plusSeconds(604800));sessions.save(refresh);return new Result(new Session(jwt,"Bearer",900,role,id.toString(),name),raw);
 }
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public Result refresh(String role,String raw){if(raw==null||raw.isBlank())throw new ApiException(401,"Sessão expirada");var s=sessions.findByTokenHash(hash(raw)).orElseThrow(()->new ApiException(401,"Sessão expirada"));if(!role.equals(s.getRole())||s.getRevoked()||s.getExpiresAt().isBefore(Instant.now()))throw new ApiException(401,"Sessão expirada");s.setRevoked(true);if("MANAGER".equals(role)){var m=managers.findById(s.getAccountId()).filter(Manager::getActive).orElseThrow(()->new ApiException(401,"Sessão expirada"));return issue(role,m.getId(),m.getName(),m.getCredentialVersion());}var a=accounts.findById(s.getAccountId()).orElseThrow(()->new ApiException(401,"Sessão expirada"));return issue(role,a.getId(),a.getCustomer().getName(),a.getCredentialVersion());}
 public void logout(String role,String raw){if(raw==null)return;var optional=sessions.findByTokenHash(hash(raw));if(optional.isEmpty())return;var s=optional.get();if(!role.equals(s.getRole()))return;revoke(role,s.getAccountId());}
 public void revoke(String role,Long id){sessions.findByRoleAndAccountId(role,id).forEach(s->s.setRevoked(true));if("MANAGER".equals(role))managers.findById(id).ifPresent(m->m.setCredentialVersion(m.getCredentialVersion()+1));else accounts.findById(id).ifPresent(a->a.setCredentialVersion(a.getCredentialVersion()+1));}
 public void changePassword(String role,Long id,PasswordChange data){if("MANAGER".equals(role)){var m=managers.findById(id).orElseThrow(ApiException::missing);if(!passwords.matches(data.currentPassword(),m.getPasswordHash()))throw new ApiException(401,"Senha atual inválida");m.setPasswordHash(passwords.encode(data.newPassword()));}else{var a=accounts.findById(id).orElseThrow(ApiException::missing);if(!passwords.matches(data.currentPassword(),a.getPasswordHash()))throw new ApiException(401,"Senha atual inválida");a.setPasswordHash(passwords.encode(data.newPassword()));}revoke(role,id);}
}
`);
java('controller/AuthController',`package br.com.florir.controller;
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
`);
java('config/BootstrapConfig',`package br.com.florir.config;
import br.com.florir.model.*;import br.com.florir.repository.*;import org.springframework.boot.CommandLineRunner;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.beans.factory.annotation.Value;import java.math.BigDecimal;import java.util.Locale;
@Component public class BootstrapConfig implements CommandLineRunner {
 private final ManagerRepository managers;private final InterestRateRepository rates;private final PasswordEncoder passwords;private final boolean enabled;private final String email,password;
 public BootstrapConfig(ManagerRepository m,InterestRateRepository r,PasswordEncoder p,@Value("$"+"{florir.bootstrap.enabled}") boolean on,@Value("$"+"{florir.bootstrap.email}") String e,@Value("$"+"{florir.bootstrap.password}") String pw){managers=m;rates=r;passwords=p;enabled=on;email=e;password=pw;}
 @Transactional public void run(String... args){if(enabled){if(email.isBlank()||password.length()<10)throw new IllegalArgumentException("Configure credenciais da gestora");String mail=email.strip().toLowerCase(Locale.ROOT);if(!managers.existsByEmail(mail)){var m=new Manager();m.setName("Gestora Florir");m.setEmail(mail);m.setPasswordHash(passwords.encode(password));managers.save(m);}}
 for(int n=1;n<=12;n++){if(rates.findByInstallmentCount(n).isEmpty()){var rate=new InterestRate();rate.setInstallmentCount(n);rate.setPercent(BigDecimal.ZERO);rates.save(rate);}}
 }
}
`);
java('controller/HealthController',`package br.com.florir.controller;
import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController public class HealthController { @GetMapping("/api/v1/health") public Map<String,String> health(){return Map.of("status","UP","application","backend-florir");} }
`);
