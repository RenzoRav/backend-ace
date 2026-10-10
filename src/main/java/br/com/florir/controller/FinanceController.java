package br.com.florir.controller;
import br.com.florir.dto.FinanceDtos.*;import br.com.florir.dto.CatalogDtos.PageView;import br.com.florir.service.FinanceService;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/manager") public class FinanceController {
 private final FinanceService service;public FinanceController(FinanceService s){service=s;}
 @GetMapping("/finance") PageView<EntryView> entries(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.entries(page,size);}
 @PostMapping("/finance") @ResponseStatus(HttpStatus.CREATED) EntryView entry(@Valid @RequestBody EntryInput d){return service.entry(d);}
 @GetMapping("/inventory-movements") PageView<StockView> stock(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.stock(page,size);}
 @GetMapping("/audit") PageView<AuditView> audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.audit(page,size);}
 @GetMapping("/reports/summary") Summary summary(){return service.summary();}
}
