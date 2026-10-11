package br.com.loja.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityEnvironmentValidator {
  private final Environment environment;
  @PostConstruct public void validate() {
    var profiles = Arrays.asList(environment.getActiveProfiles());
    boolean development = profiles.contains("dev") || profiles.contains("test");
    if (profiles.contains("prod") && development) throw new IllegalStateException("Perfis de producao e desenvolvimento incompatíveis");
    if (development) return;
    String secret = environment.getProperty("loja.jwt.secret", "");
    String lower = secret.toLowerCase(Locale.ROOT);
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32 || lower.matches(".*(development|test-only|substitua|troque|change-in-production|example|changeme).*"))
      throw new IllegalStateException("Configure um JWT_SECRET exclusivo para producao");
    URI origin;
    try { origin = URI.create(environment.getProperty("loja.allowed-origin", "")); }
    catch (IllegalArgumentException exception) { throw new IllegalStateException("Origem HTTPS obrigatoria"); }
    if (!"https".equals(origin.getScheme()) || origin.getHost() == null || origin.getUserInfo()!=null || origin.getQuery()!=null || origin.getFragment()!=null || !(origin.getPath().isEmpty()))
      throw new IllegalStateException("Configure uma origem HTTPS exata sem caminho");
    if (!environment.getProperty("loja.cookie.secure", Boolean.class, false) || environment.getProperty("loja.bootstrap.enabled", Boolean.class, false))
      throw new IllegalStateException("Producao exige cookie Secure e bootstrap desabilitado");
  }
}
