package br.com.loja.repository;

import br.com.loja.model.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Coupon e where e.id = :id")
  Optional<Coupon> lockById(@Param("id") Long id);

  Optional<Coupon> findByCode(String code);
}
