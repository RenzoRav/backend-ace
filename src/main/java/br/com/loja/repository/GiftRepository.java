package br.com.loja.repository;

import br.com.loja.model.Gift;
import org.springframework.data.jpa.repository.*;

public interface GiftRepository extends JpaRepository<Gift, Long> {}
