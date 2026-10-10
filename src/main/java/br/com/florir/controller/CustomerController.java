package br.com.florir.controller;

import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.dto.CustomerDtos.*;
import br.com.florir.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CustomerController {
  private final CustomerService service;

  public CustomerController(CustomerService s) {
    service = s;
  }

  @GetMapping("/manager/customers")
  PageView<CustomerView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.list(page, size);
  }

  @GetMapping("/manager/customers/{id}")
  CustomerView get(@PathVariable String id) {
    return service.get(id);
  }

  @PostMapping("/manager/customers")
  @ResponseStatus(HttpStatus.CREATED)
  CustomerView create(@Valid @RequestBody CustomerInput data) {
    return service.save(null, data);
  }

  @PutMapping("/manager/customers/{id}")
  CustomerView update(@PathVariable String id, @Valid @RequestBody CustomerInput data) {
    return service.save(id, data);
  }

  @GetMapping("/customer/me")
  CustomerView me() {
    return service.me();
  }

  @PutMapping("/customer/me")
  CustomerView me(@Valid @RequestBody CustomerInput data) {
    return service.updateMe(data);
  }

  @GetMapping("/customer/addresses")
  List<AddressView> addresses() {
    return service.addresses();
  }

  @PostMapping("/customer/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  AddressView createAddress(@Valid @RequestBody AddressInput data) {
    return service.address(null, data);
  }

  @PutMapping("/customer/addresses/{id}")
  AddressView updateAddress(@PathVariable String id, @Valid @RequestBody AddressInput data) {
    return service.address(id, data);
  }

  @DeleteMapping("/customer/addresses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable String id) {
    service.deleteAddress(id);
  }
}
