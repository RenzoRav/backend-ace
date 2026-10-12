package br.com.loja.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityEnvironmentValidatorTest {
  MockEnvironment production() {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    return environment
        .withProperty("loja.jwt.secret", "c5894ba84bcd43a483c7973c67fd8d27f6c5894ba84bcd43a4")
        .withProperty("loja.allowed-origin", "https://shop.example")
        .withProperty("loja.cookie.secure", "true");
  }

  @Test
  void acceptsValidProductionConfiguration() {
    assertDoesNotThrow(() -> new SecurityEnvironmentValidator(production()).validate());
  }

  @Test
  void rejectsKnownSecret() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new SecurityEnvironmentValidator(
                    production()
                        .withProperty(
                            "loja.jwt.secret",
                            "loja-development-key-change-in-production-2026-at-least-32-bytes"))
                .validate());
  }

  @Test
  void rejectsProductionWithDevelopmentProfile() {
    var environment = production();
    environment.setActiveProfiles("prod", "dev");
    assertThrows(
        IllegalStateException.class,
        () -> new SecurityEnvironmentValidator(environment).validate());
  }

  @Test
  void rejectsInsecureCookieOrOrigin() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new SecurityEnvironmentValidator(
                    production().withProperty("loja.cookie.secure", "false"))
                .validate());
    assertThrows(
        IllegalStateException.class,
        () ->
            new SecurityEnvironmentValidator(
                    production().withProperty("loja.allowed-origin", "http://localhost:3000"))
                .validate());
  }

  @Test
  void rejectsBootstrapInProduction() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new SecurityEnvironmentValidator(
                    production().withProperty("loja.bootstrap.enabled", "true"))
                .validate());
  }
}
