package br.com.florir.controller;

import br.com.florir.dto.CatalogDtos.*;
import br.com.florir.dto.DashboardDtos.*;
import br.com.florir.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService s) {
    service = s;
  }

  @GetMapping("/alerts")
  Alerts alerts() {
    return service.alerts();
  }

  @GetMapping("/alerts/low-stock")
  PageView<ProductView> low(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.lowStock(page, size);
  }

  @GetMapping("/alerts/overdue")
  PageView<InstallmentView> overdue(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.overdue(page, size);
  }

  @GetMapping("/installments")
  PageView<InstallmentView> installments(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status) {
    return service.installments(page, size, status);
  }
}
