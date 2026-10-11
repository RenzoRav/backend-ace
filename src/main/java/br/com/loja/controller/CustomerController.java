package br.com.loja.controller;

import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.dto.CustomerDtos.*;
import br.com.loja.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerController {
  private final CustomerService customerService;

  @GetMapping("/manager/customers")
  PageView<CustomerView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return customerService.list(page, size);
  }

  @GetMapping("/manager/customers/{id}")
  CustomerView get(@PathVariable String id) {
    return customerService.get(id);
  }

  @PostMapping("/manager/customers")
  @ResponseStatus(HttpStatus.CREATED)
  CustomerView create(@Valid @RequestBody CustomerInput data) {
    return customerService.save(null, data);
  }

  @PutMapping("/manager/customers/{id}")
  CustomerView update(@PathVariable String id, @Valid @RequestBody CustomerInput data) {
    return customerService.save(id, data);
  }

  @GetMapping("/customer/me")
  CustomerView me() {
    return customerService.me();
  }

  @PutMapping("/customer/me")
  CustomerView me(@Valid @RequestBody CustomerInput data) {
    return customerService.updateMe(data);
  }

  @GetMapping("/customer/addresses")
  List<AddressView> addresses() {
    return customerService.addresses();
  }

  @PostMapping("/customer/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  AddressView createAddress(@Valid @RequestBody AddressInput data) {
    return customerService.address(null, data);
  }

  @PutMapping("/customer/addresses/{id}")
  AddressView updateAddress(@PathVariable String id, @Valid @RequestBody AddressInput data) {
    return customerService.address(id, data);
  }

  @DeleteMapping("/customer/addresses/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable String id) {
    customerService.deleteAddress(id);
  }
}
