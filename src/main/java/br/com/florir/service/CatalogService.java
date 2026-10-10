package br.com.florir.service;

import br.com.florir.dto.CatalogDtos.*;
import br.com.florir.exception.ApiException;
import br.com.florir.model.*;
import br.com.florir.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
  private final ProductRepository products;
  private final BrandRepository brands;
  private final CategoryRepository categories;
  private final InventoryMovementRepository movements;
  private final AuditService audit;

  public CatalogService(
      ProductRepository p,
      BrandRepository b,
      CategoryRepository c,
      InventoryMovementRepository m,
      AuditService a) {
    products = p;
    brands = b;
    categories = c;
    movements = m;
    audit = a;
  }

  public static Long id(String id) {
    try {
      long value = Long.parseLong(id);
      if (value < 1) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException e) {
      throw new ApiException(400, "Identificador inválido");
    }
  }

  public static Pageable page(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw new ApiException(400, "Paginação inválida");
    return PageRequest.of(page, size, Sort.by("id"));
  }

  public ProductView view(Product p) {
    return new ProductView(
        p.getId().toString(),
        p.getName(),
        p.getDescription(),
        p.getImageUrl(),
        p.getPrice(),
        p.getCost(),
        p.getStock(),
        p.getMinimumStock(),
        p.getActive(),
        new Named(p.getBrand().getId().toString(), p.getBrand().getName()),
        new Named(p.getCategory().getId().toString(), p.getCategory().getName()));
  }

  public PublicProduct publicView(Product p) {
    return new PublicProduct(
        p.getId().toString(),
        p.getName(),
        p.getDescription(),
        p.getImageUrl(),
        p.getPrice(),
        p.getStock(),
        new Named(p.getBrand().getId().toString(), p.getBrand().getName()),
        new Named(p.getCategory().getId().toString(), p.getCategory().getName()));
  }

  @Transactional(readOnly = true)
  public PageView<ProductView> list(int page, int size) {
    return PageView.from(products.findAll(page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public PageView<PublicProduct> catalog(int page, int size) {
    return PageView.from(products.findByActiveTrue(page(page, size)).map(this::publicView));
  }

  @Transactional(readOnly = true)
  public ProductView get(String id) {
    return view(products.findById(id(id)).orElseThrow(ApiException::missing));
  }

  public ProductView save(String id, ProductInput data) {
    var p =
        id == null ? new Product() : products.lockById(id(id)).orElseThrow(ApiException::missing);
    int old = p.getStock();
    p.setName(data.name().strip());
    p.setDescription(data.description());
    p.setImageUrl(data.imageUrl());
    p.setPrice(PricingCalculator.money(data.price()));
    p.setCost(PricingCalculator.money(data.cost()));
    p.setMinimumStock(data.minimumStock());
    p.setActive(data.active());
    p.setBrand(brands.findById(id(data.brandId())).orElseThrow(ApiException::missing));
    p.setCategory(categories.findById(id(data.categoryId())).orElseThrow(ApiException::missing));
    p.setStock(data.stock());
    products.save(p);
    stockMovement(
        p,
        data.stock() - old,
        id == null ? "INITIAL" : "ADJUSTMENT",
        "Cadastro/edição pela gestora");
    audit.record(id == null ? "CREATE" : "UPDATE", "Product", p.getId());
    return view(p);
  }

  public ProductView adjust(String id, StockAdjustment data) {
    var p = products.lockById(id(id)).orElseThrow(ApiException::missing);
    int delta = data.stock() - p.getStock();
    p.setStock(data.stock());
    stockMovement(p, delta, "ADJUSTMENT", data.reason());
    audit.record("ADJUST_STOCK", "Product", p.getId());
    return view(p);
  }

  private void stockMovement(Product p, int delta, String type, String reason) {
    if (delta == 0) return;
    var movement = new InventoryMovement();
    movement.setProduct(p);
    movement.setType(type);
    movement.setQuantity(delta);
    movement.setDescription(reason);
    movements.save(movement);
  }

  public void deactivate(String id) {
    var p = products.lockById(id(id)).orElseThrow(ApiException::missing);
    p.setActive(false);
    audit.record("DEACTIVATE", "Product", p.getId());
  }

  @Transactional(readOnly = true)
  public List<Named> brands() {
    return brands.findAll(Sort.by("name")).stream()
        .map(b -> new Named(b.getId().toString(), b.getName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Named> categories() {
    return categories.findAll(Sort.by("name")).stream()
        .map(c -> new Named(c.getId().toString(), c.getName()))
        .toList();
  }

  public Named brand(String id, NameInput data) {
    var b = id == null ? new Brand() : brands.findById(id(id)).orElseThrow(ApiException::missing);
    if ((id == null || !b.getName().equalsIgnoreCase(data.name().strip()))
        && brands.existsByNameIgnoreCase(data.name().strip()))
      throw ApiException.conflict("Marca já cadastrada");
    b.setName(data.name().strip());
    brands.save(b);
    audit.record(id == null ? "CREATE" : "UPDATE", "Brand", b.getId());
    return new Named(b.getId().toString(), b.getName());
  }

  public Named category(String id, NameInput data) {
    var c =
        id == null
            ? new Category()
            : categories.findById(id(id)).orElseThrow(ApiException::missing);
    if ((id == null || !c.getName().equalsIgnoreCase(data.name().strip()))
        && categories.existsByNameIgnoreCase(data.name().strip()))
      throw ApiException.conflict("Categoria já cadastrada");
    c.setName(data.name().strip());
    categories.save(c);
    audit.record(id == null ? "CREATE" : "UPDATE", "Category", c.getId());
    return new Named(c.getId().toString(), c.getName());
  }

  public void deleteBrand(String id) {
    var b = brands.findById(id(id)).orElseThrow(ApiException::missing);
    audit.record("DELETE", "Brand", b.getId());
    brands.delete(b);
  }

  public void deleteCategory(String id) {
    var c = categories.findById(id(id)).orElseThrow(ApiException::missing);
    audit.record("DELETE", "Category", c.getId());
    categories.delete(c);
  }
}
