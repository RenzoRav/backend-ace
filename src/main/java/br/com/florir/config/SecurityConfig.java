package br.com.florir.config;

import br.com.florir.repository.*;
import br.com.florir.security.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  JwtEncoder encoder(@Value("$" + "{florir.jwt.secret}") String secret) {
    if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes");
    return new NimbusJwtEncoder(
        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
            secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  @Bean
  JwtDecoder decoder(
      @Value("$" + "{florir.jwt.secret}") String secret,
      @Value("$" + "{florir.jwt.issuer}") String issuer,
      @Value("$" + "{florir.jwt.audience}") String audience) {
    var decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    OAuth2TokenValidator<Jwt> audienceValidator =
        jwtToken ->
            jwtToken.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ManagerRepository managerRepository,
      CustomerAccountRepository customerAccountRepository,
      @Value("$" + "{florir.allowed-origin}") String origin)
      throws Exception {
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("role");
    authorities.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    http.csrf(configuration -> configuration.disable())
        .cors(configuration -> {})
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorizationRules ->
                authorizationRules
                    .requestMatchers("/api/v1/auth/**", "/api/v1/catalog/**", "/api/v1/health")
                    .permitAll()
                    .requestMatchers("/api/v1/manager/**")
                    .hasRole("MANAGER")
                    .requestMatchers("/api/v1/customer/**")
                    .hasRole("CUSTOMER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer.jwt(jwtToken -> jwtToken.jwtAuthenticationConverter(converter)))
        .addFilterBefore(new AuthOriginFilter(origin), BearerTokenAuthenticationFilter.class)
        .addFilterAfter(
            new CredentialVersionFilter(managerRepository, customerAccountRepository),
            BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("$" + "{florir.allowed-origin}") String origin) {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(origin));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Idempotency-Key"));
    configuration.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
