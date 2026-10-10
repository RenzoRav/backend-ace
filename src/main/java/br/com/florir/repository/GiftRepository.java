package br.com.florir.repository;

import br.com.florir.model.Gift;
import org.springframework.data.jpa.repository.*;

public interface GiftRepository extends JpaRepository<Gift, Long> {}
