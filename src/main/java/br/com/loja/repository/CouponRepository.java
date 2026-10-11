package br.com.loja.repository;

import br.com.loja.model.Coupon;
import org.springframework.data.jpa.repository.*;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Coupon e where e.id = :id")
  java.util.Optional<Coupon> lockById(
      @org.springframework.data.repository.query.Param("id") Long id);

  java.util.Optional<Coupon> findByCode(String code);
}
