package br.com.florir.config;
import br.com.florir.model.*;import br.com.florir.repository.*;import org.springframework.boot.CommandLineRunner;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.beans.factory.annotation.Value;import java.math.BigDecimal;import java.util.Locale;
@Component public class BootstrapConfig implements CommandLineRunner {
 private final ManagerRepository managers;private final InterestRateRepository rates;private final PasswordEncoder passwords;private final boolean enabled;private final String email,password;
 public BootstrapConfig(ManagerRepository m,InterestRateRepository r,PasswordEncoder p,@Value("$"+"{florir.bootstrap.enabled}") boolean on,@Value("$"+"{florir.bootstrap.email}") String e,@Value("$"+"{florir.bootstrap.password}") String pw){managers=m;rates=r;passwords=p;enabled=on;email=e;password=pw;}
 @Transactional public void run(String... args){if(enabled){if(email.isBlank()||password.length()<10)throw new IllegalArgumentException("Configure credenciais da gestora");String mail=email.strip().toLowerCase(Locale.ROOT);if(!managers.existsByEmail(mail)){var m=new Manager();m.setName("Gestora Florir");m.setEmail(mail);m.setPasswordHash(passwords.encode(password));managers.save(m);}}
 for(int n=1;n<=12;n++){if(rates.findByInstallmentCount(n).isEmpty()){var rate=new InterestRate();rate.setInstallmentCount(n);rate.setPercent(BigDecimal.ZERO);rates.save(rate);}}
 }
}
