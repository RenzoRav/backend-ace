package br.com.loja.controller;

import br.com.loja.dto.AuthDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.security.ActorService;
import br.com.loja.service.AuthService;
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

  public AuthController(
      AuthService authService,
      ActorService actorService,
      @Value("$" + "{loja.cookie.secure}") boolean secureCookie) {
    this.authService = authService;
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
    return result(realm, authService.login(role(realm), data), 200);
  }

  @PostMapping("/customer/register")
  ResponseEntity<Session> register(@Valid @RequestBody Register data) {
    return result("customer", authService.register(data), 201);
  }

  @PostMapping("/{realm}/refresh")
  ResponseEntity<Session> refresh(@PathVariable String realm, HttpServletRequest request) {
    return result(realm, authService.refresh(role(realm), raw(realm, request)), 200);
  }

  @PostMapping("/{realm}/logout")
  ResponseEntity<Void> logout(@PathVariable String realm, HttpServletRequest request) {
    authService.logout(role(realm), raw(realm, request));
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(realm, "", 0)).build();
  }

  @PostMapping("/{realm}/password")
  ResponseEntity<Void> password(
      @PathVariable String realm, @Valid @RequestBody PasswordChange data) {
    if (!role(realm).equals(actorService.role())) throw new ApiException(403, "Acesso negado");
    authService.changePassword(role(realm), actorService.accountId(), data);
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie(realm, "", 0)).build();
  }
}
