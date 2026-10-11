package br.com.loja.controller;

import br.com.loja.dto.CatalogDtos.*;
import br.com.loja.dto.DashboardDtos.*;
import br.com.loja.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;

  @GetMapping("/alerts")
  Alerts alerts() {
    return dashboardService.alerts();
  }

  @GetMapping("/alerts/low-stock")
  PageView<ProductView> low(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return dashboardService.lowStock(page, size);
  }

  @GetMapping("/alerts/overdue")
  PageView<InstallmentView> overdue(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return dashboardService.overdue(page, size);
  }

  @GetMapping("/installments")
  PageView<InstallmentView> installments(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status) {
    return dashboardService.installments(page, size, status);
  }
}
