package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.*;
import br.com.florir.dto.DashboardDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.Installment;
import br.com.florir.repository.*;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {
  private final ProductRepository productRepository;
  private final InstallmentRepository installmentRepository;
  private final CatalogService catalogService;

  private InstallmentView view(Installment installment) {
    var purchaseOrder = installment.getOrder();
    return new InstallmentView(
        installment.getId().toString(),
        purchaseOrder.getId().toString(),
        purchaseOrder.getCustomer().getId().toString(),
        purchaseOrder.getCustomerName(),
        purchaseOrder.getCustomerPhone(),
        installment.getNumber(),
        installment.getAmount(),
        installment.getDueDate(),
        installment.getStatus(),
        installment.getPaidAt());
  }

  public Alerts alerts() {
    return new Alerts(
        productRepository.lowStockCount(),
        installmentRepository.countByStatusAndDueDateBefore("PENDING", LocalDate.now()));
  }

  public PageView<ProductView> lowStock(int page, int size) {
    return PageView.from(
        productRepository.lowStock(CatalogService.page(page, size)).map(catalogService::view));
  }

  public PageView<InstallmentView> overdue(int page, int size) {
    return PageView.from(
        installmentRepository
            .findByStatusAndDueDateBefore(
                "PENDING", LocalDate.now(), CatalogService.page(page, size))
            .map(this::view));
  }

  public PageView<InstallmentView> installments(int page, int size, String status) {
    if (status != null && !List.of("PENDING", "PAID", "CANCELLED").contains(status))
      throw new ApiException(400, "Status inválido");
    var paging = CatalogService.page(page, size);
    return PageView.from(
        (status == null
                ? installmentRepository.findAll(paging)
                : installmentRepository.findByStatus(status, paging))
            .map(this::view));
  }
}
