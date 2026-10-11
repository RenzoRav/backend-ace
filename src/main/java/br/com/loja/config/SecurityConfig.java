package br.com.loja.config;

import br.com.loja.repository.*;
import br.com.loja.security.*;
import br.com.loja.service.AuthenticationLimitService;
import br.com.loja.service.SecurityEventService;
import br.com.loja.service.SessionFamilyService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
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
@EnableScheduling
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  JwtEncoder encoder(@Value("$" + "{loja.jwt.secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes");
    return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
  }

  @Bean
  JwtDecoder decoder(
      @Value("$" + "{loja.jwt.secret}") String secret,
      @Value("$" + "{loja.jwt.issuer}") String issuer,
      @Value("$" + "{loja.jwt.audience}") String audience) {
    var decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
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
      AuthenticationLimitService authenticationLimitService,
      SecurityEventService securityEventService,
      SessionFamilyService sessionFamilyService,
      Environment environment,
      ManagerRepository managerRepository,
      CustomerAccountRepository customerAccountRepository,
      @Value("$" + "{loja.allowed-origin}") String origin)
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
        .addFilterBefore(new RequestBodyLimitFilter(262144), BearerTokenAuthenticationFilter.class)
        .addFilterBefore(new AuthOriginFilter(origin), BearerTokenAuthenticationFilter.class)
        .addFilterBefore(new AuthenticationLimitFilter(authenticationLimitService,securityEventService,environment), BearerTokenAuthenticationFilter.class)
        .addFilterAfter(
            new CredentialVersionFilter(sessionFamilyService,managerRepository, customerAccountRepository),
            BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("$" + "{loja.allowed-origin}") String origin) {
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



