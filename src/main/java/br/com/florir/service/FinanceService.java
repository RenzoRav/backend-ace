package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.FinanceDtos.*;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinanceService {
  private final FinancialMovementRepository entries;
  private final InventoryMovementRepository stock;
  private final AuditLogRepository logs;
  private final CustomerRepository customers;
  private final ProductRepository products;
  private final PurchaseOrderRepository orders;
  private final AuditService audit;

  public FinanceService(
      FinancialMovementRepository e,
      InventoryMovementRepository st,
      AuditLogRepository l,
      CustomerRepository c,
      ProductRepository p,
      PurchaseOrderRepository o,
      AuditService a) {
    entries = e;
    stock = st;
    logs = l;
    customers = c;
    products = p;
    orders = o;
    audit = a;
  }

  private EntryView view(FinancialMovement m) {
    return new EntryView(
        m.getId().toString(),
        m.getOrder() == null ? null : m.getOrder().getId().toString(),
        m.getInstallment() == null ? null : m.getInstallment().getId().toString(),
        m.getType(),
        m.getDescription(),
        m.getAmount(),
        m.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public PageView<EntryView> entries(int page, int size) {
    return PageView.from(entries.findAll(CatalogService.page(page, size)).map(this::view));
  }

  public EntryView entry(EntryInput d) {
    var m = new FinancialMovement();
    m.setType(d.type());
    m.setDescription(d.description());
    m.setAmount(PricingCalculator.money(d.amount()));
    entries.save(m);
    audit.record("CREATE_MANUAL", "FinancialMovement", m.getId());
    return view(m);
  }

  @Transactional(readOnly = true)
  public PageView<StockView> stock(int page, int size) {
    return PageView.from(
        stock
            .findAll(CatalogService.page(page, size))
            .map(
                m ->
                    new StockView(
                        m.getId().toString(),
                        m.getProduct().getId().toString(),
                        m.getOrder() == null ? null : m.getOrder().getId().toString(),
                        m.getType(),
                        m.getQuantity(),
                        m.getDescription(),
                        m.getCreatedAt())));
  }

  @Transactional(readOnly = true)
  public PageView<AuditView> audit(int page, int size) {
    return PageView.from(
        logs.findAll(CatalogService.page(page, size))
            .map(
                l ->
                    new AuditView(
                        l.getId().toString(),
                        l.getManager().getId().toString(),
                        l.getManager().getName(),
                        l.getAction(),
                        l.getEntity(),
                        l.getEntityId(),
                        l.getCreatedAt())));
  }

  @Transactional(readOnly = true)
  public Summary summary() {
    var in = entries.sumAmount("IN");
    var out = entries.sumAmount("OUT");
    return new Summary(
        customers.count(),
        products.count(),
        orders.count(),
        PricingCalculator.money(in),
        PricingCalculator.money(out),
        PricingCalculator.money(in.subtract(out)),
        PricingCalculator.money(products.stockCost()));
  }
}
