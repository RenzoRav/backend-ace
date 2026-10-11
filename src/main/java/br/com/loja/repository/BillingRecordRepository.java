package br.com.loja.repository;

import br.com.loja.model.BillingRecord;
import org.springframework.data.jpa.repository.*;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {}
