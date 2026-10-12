package br.com.loja.controller;

import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.dto.OrderDtos.*;
import br.com.loja.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  @GetMapping("/manager/orders")
  PageView<OrderView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return orderService.list(page, size);
  }

  @GetMapping("/customer/orders/paged")
  PageView<OrderView> minePage(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return orderService.myOrdersPage(page, size);
  }

  @GetMapping("/customer/orders")
  List<OrderView> mine() {
    return orderService.myOrders();
  }

  @GetMapping({"/manager/orders/{id}", "/customer/orders/{id}"})
  OrderView get(@PathVariable String id) {
    return orderService.get(id);
  }

  @PostMapping({"/manager/orders", "/customer/orders"})
  @ResponseStatus(HttpStatus.CREATED)
  OrderView create(
      @Valid @RequestBody OrderInput data,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return orderService.create(data, idempotencyKey);
  }

  @PostMapping("/manager/orders/{id}/confirm")
  OrderView confirm(@PathVariable String id) {
    return orderService.confirm(id);
  }

  @PatchMapping("/manager/orders/{id}/status")
  OrderView status(@PathVariable String id, @Valid @RequestBody StatusInput data) {
    return orderService.status(id, data.status());
  }

  @PostMapping("/manager/orders/{id}/receive")
  OrderView receive(@PathVariable String id) {
    return orderService.receive(id);
  }

  @PostMapping("/customer/orders/{id}/simulate-payment")
  OrderView simulate(@PathVariable String id) {
    return orderService.simulate(id);
  }

  @PostMapping({"/manager/orders/{id}/cancel", "/customer/orders/{id}/cancel"})
  OrderView cancel(@PathVariable String id) {
    return orderService.cancel(id);
  }

  @PostMapping("/manager/installments/{id}/receive")
  OrderView pay(@PathVariable String id) {
    return orderService.payInstallment(id);
  }
}
