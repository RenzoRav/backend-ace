package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.*;
import br.com.florir.dto.DashboardDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.Installment;
import br.com.florir.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
  private final ProductRepository products;
  private final InstallmentRepository installments;
  private final CatalogService catalog;

  public DashboardService(ProductRepository p, InstallmentRepository i, CatalogService c) {
    products = p;
    installments = i;
    catalog = c;
  }

  private InstallmentView view(Installment i) {
    var o = i.getOrder();
    return new InstallmentView(
        i.getId().toString(),
        o.getId().toString(),
        o.getCustomer().getId().toString(),
        o.getCustomerName(),
        o.getCustomerPhone(),
        i.getNumber(),
        i.getAmount(),
        i.getDueDate(),
        i.getStatus(),
        i.getPaidAt());
  }

  public Alerts alerts() {
    return new Alerts(
        products.lowStockCount(),
        installments.countByStatusAndDueDateBefore("PENDING", LocalDate.now()));
  }

  public PageView<ProductView> lowStock(int page, int size) {
    return PageView.from(products.lowStock(CatalogService.page(page, size)).map(catalog::view));
  }

  public PageView<InstallmentView> overdue(int page, int size) {
    return PageView.from(
        installments
            .findByStatusAndDueDateBefore(
                "PENDING", LocalDate.now(), CatalogService.page(page, size))
            .map(this::view));
  }

  public PageView<InstallmentView> installments(int page, int size, String status) {
    if (status != null && !List.of("PENDING", "PAID", "CANCELLED").contains(status))
      throw new ApiException(400, "Status inválido");
    var paging = CatalogService.page(page, size);
    return PageView.from(
        (status == null ? installments.findAll(paging) : installments.findByStatus(status, paging))
            .map(this::view));
  }
}
