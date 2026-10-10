package br.com.florir.controller;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.FinanceDtos.*;
import br.com.florir.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class FinanceController {
  private final FinanceService financeService;

  @GetMapping("/finance")
  PageView<EntryView> entries(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return financeService.entries(page, size);
  }

  @PostMapping("/finance")
  @ResponseStatus(HttpStatus.CREATED)
  EntryView entry(@Valid @RequestBody EntryInput entryInput) {
    return financeService.entry(entryInput);
  }

  @GetMapping("/inventory-movements")
  PageView<StockView> stock(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return financeService.stock(page, size);
  }

  @GetMapping("/audit")
  PageView<AuditView> audit(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return financeService.audit(page, size);
  }

  @GetMapping("/reports/summary")
  Summary summary() {
    return financeService.summary();
  }
}
