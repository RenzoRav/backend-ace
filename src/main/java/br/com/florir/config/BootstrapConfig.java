package br.com.florir.config;

import br.com.florir.model.*;
import br.com.florir.repository.*;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapConfig implements CommandLineRunner {
  private final ManagerRepository managerRepository;
  private final InterestRateRepository interestRateRepository;
  private final PasswordEncoder passwordEncoder;
  private final boolean enabled;
  private final String email, password;

  public BootstrapConfig(
      ManagerRepository managerRepository,
      InterestRateRepository interestRateRepository,
      PasswordEncoder passwordEncoder,
      @Value("$" + "{florir.bootstrap.enabled}") boolean bootstrapEnabled,
      @Value("$" + "{florir.bootstrap.email}") String managerEmail,
      @Value("$" + "{florir.bootstrap.password}") String managerPassword) {
    this.managerRepository = managerRepository;
    this.interestRateRepository = interestRateRepository;
    this.passwordEncoder = passwordEncoder;
    enabled = bootstrapEnabled;
    email = managerEmail;
    password = managerPassword;
  }

  @Transactional
  public void run(String... args) {
    if (enabled) {
      if (email.isBlank() || password.length() < 10)
        throw new IllegalArgumentException("Configure credenciais da gestora");
      String mail = email.strip().toLowerCase(Locale.ROOT);
      if (!managerRepository.existsByEmail(mail)) {
        var manager = new Manager();
        manager.setName("Gestora Florir");
        manager.setEmail(mail);
        manager.setPasswordHash(passwordEncoder.encode(password));
        managerRepository.save(manager);
      }
    }
    for (int installmentCount = 1; installmentCount <= 12; installmentCount++) {
      if (interestRateRepository.findByInstallmentCount(installmentCount).isEmpty()) {
        var rate = new InterestRate();
        rate.setInstallmentCount(installmentCount);
        rate.setPercent(BigDecimal.ZERO);
        interestRateRepository.save(rate);
      }
    }
  }
}
