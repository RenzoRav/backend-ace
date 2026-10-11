package br.com.loja.controller;

import br.com.loja.dto.AuthDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.security.ActorService;
import br.com.loja.service.AuthService;
import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  private final ActorService actorService;
  private final boolean secure;
  private final AuthenticationLimitService authenticationLimitService;
  private final SecurityEventService securityEventService;
  private final int accountMaximum;

  public AuthController(
      AuthService authService,
      AuthenticationLimitService authenticationLimitService,
      SecurityEventService securityEventService,
      @Value("${loja.limits.login-account:10}") int accountMaximum,
      ActorService actorService,
      @Value("$" + "{loja.cookie.secure}") boolean secureCookie) {
    this.authService = authService;
    this.authenticationLimitService=authenticationLimitService;
    this.securityEventService=securityEventService;
    if(accountMaximum<1||accountMaximum>100000)throw new IllegalStateException("Limite de conta inválido");
    this.accountMaximum=accountMaximum;
    this.actorService = actorService;
    secure = secureCookie;
  }

  private String role(String realm) {
    return switch (realm) {
      case "manager" -> "MANAGER";
      case "customer" -> "CUSTOMER";
      default -> throw ApiException.missing();
    };
  }

  private String cookieName(String realm) {
    return "loja_" + realm + "_refresh";
  }

  private String cookie(String realm, String value, long age) {
    return ResponseCookie.from(cookieName(realm), value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/api/v1/auth/" + realm)
        .maxAge(Duration.ofSeconds(age))
        .build()
        .toString();
  }

  private String raw(String realm, HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (var cookie : request.getCookies())
      if (cookieName(realm).equals(cookie.getName())) return cookie.getValue();
    return null;
  }

  private ResponseEntity<Session> result(
      String realm, AuthService.Result authenticationResult, int status) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, cookie(realm, authenticationResult.refreshToken(), 604800))
        .body(authenticationResult.session());
  }

  @PostMapping("/{realm}/login")
  ResponseEntity<Session> login(@PathVariable String realm, @Valid @RequestBody Login data) {
    var decision=authenticationLimitService.check("login-account",realm+":"+data.email().strip().toLowerCase(Locale.ROOT),accountMaximum,Duration.ofMinutes(5));
    if(!decision.allowed())return ResponseEntity.status(429).header("Retry-After",Long.toString(decision.retryAfterSeconds())).build();
    try{
      var authenticationResult=authService.login(role(realm),data);
      securityEventService.record("LOGIN","ALLOWED",authenticationResult.session().accountId(),UUID.randomUUID().toString());
      return result(realm,authenticationResult,200);
    }catch(ApiException exception){securityEventService.record("LOGIN","DENIED",data.email().strip().toLowerCase(Locale.ROOT),UUID.randomUUID().toString());throw exception;}
  }

  @PostMapping("/customer/register")
  ResponseEntity<Session> register(@Valid @RequestBody Register data) {
    var authenticationResult=authService.register(data);
    securityEventService.record("REGISTER","ALLOWED",authenticationResult.session().accountId(),UUID.randomUUID().toString());
    return result("customer", authenticationResult, 201);
  }

  @PostMapping("/{realm}/refresh")
  ResponseEntity<Session> refresh(@PathVariable String realm, HttpServletRequest request) {
    var authenticationResult=authService.refresh(role(realm),raw(realm,request));
    securityEventService.record("REFRESH","ALLOWED",authenticationResult.session().accountId(),UUID.randomUUID().toString());
    return result(realm,authenticationResult,200);
  }

  @PostMapping("/{realm}/logout")
  ResponseEntity<Void> logout(@PathVariable String realm, HttpServletRequest request) {
    authService.logout(role(realm), raw(realm, request));
    securityEventService.record("LOGOUT","ALLOWED",realm,UUID.randomUUID().toString());
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(realm, "", 0)).build();
  }

  @PostMapping("/{realm}/password")
  ResponseEntity<Void> password(
      @PathVariable String realm, @Valid @RequestBody PasswordChange data) {
    if (!role(realm).equals(actorService.role())) throw new ApiException(403, "Acesso negado");
    var decision=authenticationLimitService.check("password-account",realm+":"+actorService.accountId(),5,Duration.ofMinutes(5));
    if(!decision.allowed())return ResponseEntity.status(429).header("Retry-After",Long.toString(decision.retryAfterSeconds())).build();
    try{authService.changePassword(role(realm),actorService.accountId(),data);securityEventService.record("PASSWORD","ALLOWED",realm+":"+actorService.accountId(),UUID.randomUUID().toString());}
    catch(ApiException exception){securityEventService.record("PASSWORD","DENIED",realm+":"+actorService.accountId(),UUID.randomUUID().toString());throw exception;}
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie(realm, "", 0)).build();
  }
}


