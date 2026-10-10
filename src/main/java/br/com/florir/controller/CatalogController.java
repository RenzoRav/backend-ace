package br.com.florir.controller;

import br.com.florir.dto.CatalogDtos.*;
import br.com.florir.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
  private final CatalogService service;

  public CatalogController(CatalogService s) {
    service = s;
  }

  @GetMapping("/catalog/products")
  PageView<PublicProduct> catalog(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String brandId,
      @RequestParam(required = false) String categoryId) {
    return service.catalog(page, size, search, brandId, categoryId);
  }

  @GetMapping({"/catalog/brands", "/manager/brands"})
  List<Named> brands() {
    return service.brands();
  }

  @GetMapping({"/catalog/categories", "/manager/categories"})
  List<Named> categories() {
    return service.categories();
  }

  @GetMapping("/manager/products")
  PageView<ProductView> products(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.list(page, size);
  }

  @GetMapping("/manager/products/{id}")
  ProductView product(@PathVariable String id) {
    return service.get(id);
  }

  @PostMapping("/manager/products")
  @ResponseStatus(HttpStatus.CREATED)
  ProductView create(@Valid @RequestBody ProductInput data) {
    return service.save(null, data);
  }

  @PutMapping("/manager/products/{id}")
  ProductView update(@PathVariable String id, @Valid @RequestBody ProductInput data) {
    return service.save(id, data);
  }

  @PostMapping("/manager/products/{id}/stock")
  ProductView stock(@PathVariable String id, @Valid @RequestBody StockAdjustment data) {
    return service.adjust(id, data);
  }

  @DeleteMapping("/manager/products/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivate(@PathVariable String id) {
    service.deactivate(id);
  }

  @PostMapping("/manager/brands")
  @ResponseStatus(HttpStatus.CREATED)
  Named brand(@Valid @RequestBody NameInput data) {
    return service.brand(null, data);
  }

  @PutMapping("/manager/brands/{id}")
  Named brand(@PathVariable String id, @Valid @RequestBody NameInput data) {
    return service.brand(id, data);
  }

  @DeleteMapping("/manager/brands/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteBrand(@PathVariable String id) {
    service.deleteBrand(id);
  }

  @PostMapping("/manager/categories")
  @ResponseStatus(HttpStatus.CREATED)
  Named category(@Valid @RequestBody NameInput data) {
    return service.category(null, data);
  }

  @PutMapping("/manager/categories/{id}")
  Named category(@PathVariable String id, @Valid @RequestBody NameInput data) {
    return service.category(id, data);
  }

  @DeleteMapping("/manager/categories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteCategory(@PathVariable String id) {
    service.deleteCategory(id);
  }

  @GetMapping("/catalog/products/{id}")
  PublicProduct detail(@PathVariable String id) {
    return service.detail(id);
  }
}
