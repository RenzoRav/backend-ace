package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.FinanceDtos.*;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FinanceService {
  private final FinancialMovementRepository financialMovementRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final AuditLogRepository auditLogRepository;
  private final CustomerRepository customerRepository;
  private final ProductRepository productRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final AuditService auditService;

  private EntryView view(FinancialMovement financialMovement) {
    return new EntryView(
        financialMovement.getId().toString(),
        financialMovement.getOrder() == null
            ? null
            : financialMovement.getOrder().getId().toString(),
        financialMovement.getInstallment() == null
            ? null
            : financialMovement.getInstallment().getId().toString(),
        financialMovement.getType(),
        financialMovement.getDescription(),
        financialMovement.getAmount(),
        financialMovement.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public PageView<EntryView> entries(int page, int size) {
    return PageView.from(
        financialMovementRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  public EntryView entry(EntryInput entryInput) {
    var financialMovement = new FinancialMovement();
    financialMovement.setType(entryInput.type());
    financialMovement.setDescription(entryInput.description());
    financialMovement.setAmount(PricingCalculator.money(entryInput.amount()));
    financialMovementRepository.save(financialMovement);
    auditService.record("CREATE_MANUAL", "FinancialMovement", financialMovement.getId());
    return view(financialMovement);
  }

  @Transactional(readOnly = true)
  public PageView<StockView> stock(int page, int size) {
    return PageView.from(
        inventoryMovementRepository
            .findAll(CatalogService.page(page, size))
            .map(
                inventoryMovement ->
                    new StockView(
                        inventoryMovement.getId().toString(),
                        inventoryMovement.getProduct().getId().toString(),
                        inventoryMovement.getOrder() == null
                            ? null
                            : inventoryMovement.getOrder().getId().toString(),
                        inventoryMovement.getType(),
                        inventoryMovement.getQuantity(),
                        inventoryMovement.getDescription(),
                        inventoryMovement.getCreatedAt())));
  }

  @Transactional(readOnly = true)
  public PageView<AuditView> audit(int page, int size) {
    return PageView.from(
        auditLogRepository
            .findAll(CatalogService.page(page, size))
            .map(
                auditLog ->
                    new AuditView(
                        auditLog.getId().toString(),
                        auditLog.getManager().getId().toString(),
                        auditLog.getManager().getName(),
                        auditLog.getAction(),
                        auditLog.getEntity(),
                        auditLog.getEntityId(),
                        auditLog.getCreatedAt())));
  }

  @Transactional(readOnly = true)
  public Summary summary() {
    var incomingAmount = financialMovementRepository.sumAmount("IN");
    var outgoingAmount = financialMovementRepository.sumAmount("OUT");
    return new Summary(
        customerRepository.count(),
        productRepository.count(),
        purchaseOrderRepository.count(),
        PricingCalculator.money(incomingAmount),
        PricingCalculator.money(outgoingAmount),
        PricingCalculator.money(incomingAmount.subtract(outgoingAmount)),
        PricingCalculator.money(productRepository.stockCost()));
  }
}
