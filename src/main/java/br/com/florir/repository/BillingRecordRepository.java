package br.com.florir.repository;
import br.com.florir.model.BillingRecord;import org.springframework.data.jpa.repository.*;
public interface BillingRecordRepository extends JpaRepository<BillingRecord,Long> {
}
