package br.com.loja.service;

import br.com.loja.dto.CatalogDtos.*;
import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CatalogService {
  private final ProductRepository productRepository;
  private final BrandRepository brandRepository;
  private final CategoryRepository categoryRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final AuditService auditService;

  public static Long id(String id) {
    try {
      long value = Long.parseLong(id);
      if (value < 1) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException exception) {
      throw new ApiException(400, "Identificador inválido");
    }
  }

  public static Pageable page(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw new ApiException(400, "Paginação inválida");
    return PageRequest.of(page, size, Sort.by("id"));
  }

  public ProductView view(Product product) {
    return new ProductView(
        product.getId().toString(),
        product.getName(),
        product.getDescription(),
        product.getImageUrl(),
        product.getPrice(),
        product.getCost(),
        product.getStock(),
        product.getMinimumStock(),
        product.getActive(),
        new Named(product.getBrand().getId().toString(), product.getBrand().getName()),
        new Named(product.getCategory().getId().toString(), product.getCategory().getName()));
  }

  public PublicProduct publicView(Product product) {
    return new PublicProduct(
        product.getId().toString(),
        product.getName(),
        product.getDescription(),
        product.getImageUrl(),
        product.getPrice(),
        product.getStock(),
        new Named(product.getBrand().getId().toString(), product.getBrand().getName()),
        new Named(product.getCategory().getId().toString(), product.getCategory().getName()));
  }

  @Transactional(readOnly = true)
  public PageView<ProductView> list(int page, int size) {
    return PageView.from(productRepository.findAll(page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public PageView<PublicProduct> catalog(
      int page, int size, String search, String brandId, String categoryId) {
    org.springframework.data.jpa.domain.Specification<Product> productSpecification =
        (root, query, builder) -> builder.isTrue(root.get("active"));
    if (search != null && !search.isBlank()) {
      if (search.length() > 255) throw new ApiException(400, "Pesquisa muito longa");
      String value = "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
      productSpecification =
          productSpecification.and(
              (root, query, builder) -> builder.like(builder.lower(root.get("name")), value));
    }
    if (brandId != null) {
      Long brand = id(brandId);
      productSpecification =
          productSpecification.and(
              (root, query, builder) -> builder.equal(root.get("brand").get("id"), brand));
    }
    if (categoryId != null) {
      Long category = id(categoryId);
      productSpecification =
          productSpecification.and(
              (root, query, builder) -> builder.equal(root.get("category").get("id"), category));
    }
    return PageView.from(
        productRepository.findAll(productSpecification, page(page, size)).map(this::publicView));
  }

  @Transactional(readOnly = true)
  public ProductView get(String id) {
    return view(productRepository.findById(id(id)).orElseThrow(ApiException::missing));
  }

  public ProductView save(String id, ProductInput data) {
    var product =
        id == null
            ? new Product()
            : productRepository.lockById(id(id)).orElseThrow(ApiException::missing);
    int previousStock = product.getStock();
    product.setName(data.name().strip());
    product.setDescription(data.description());
    product.setImageUrl(data.imageUrl());
    product.setPrice(PricingCalculator.money(data.price()));
    product.setCost(PricingCalculator.money(data.cost()));
    product.setMinimumStock(data.minimumStock());
    product.setActive(data.active());
    product.setBrand(
        brandRepository.findById(id(data.brandId())).orElseThrow(ApiException::missing));
    product.setCategory(
        categoryRepository.findById(id(data.categoryId())).orElseThrow(ApiException::missing));
    product.setStock(data.stock());
    productRepository.save(product);
    stockMovement(
        product,
        data.stock() - previousStock,
        id == null ? "INITIAL" : "ADJUSTMENT",
        "Cadastro/edição pela gestora");
    auditService.record(id == null ? "CREATE" : "UPDATE", "Product", product.getId());
    return view(product);
  }

  public ProductView adjust(String id, StockAdjustment data) {
    var product = productRepository.lockById(id(id)).orElseThrow(ApiException::missing);
    int delta = data.stock() - product.getStock();
    product.setStock(data.stock());
    stockMovement(product, delta, "ADJUSTMENT", data.reason());
    auditService.record("ADJUST_STOCK", "Product", product.getId());
    return view(product);
  }

  private void stockMovement(Product product, int delta, String type, String reason) {
    if (delta == 0) return;
    var movement = new InventoryMovement();
    movement.setProduct(product);
    movement.setType(type);
    movement.setQuantity(delta);
    movement.setDescription(reason);
    inventoryMovementRepository.save(movement);
  }

  public void deactivate(String id) {
    var product = productRepository.lockById(id(id)).orElseThrow(ApiException::missing);
    product.setActive(false);
    auditService.record("DEACTIVATE", "Product", product.getId());
  }

  @Transactional(readOnly = true)
  public List<Named> brands() {
    return brandRepository.findAll(Sort.by("name")).stream()
        .map(brand -> new Named(brand.getId().toString(), brand.getName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Named> categories() {
    return categoryRepository.findAll(Sort.by("name")).stream()
        .map(category -> new Named(category.getId().toString(), category.getName()))
        .toList();
  }

  public Named brand(String id, NameInput data) {
    var brand =
        id == null
            ? new Brand()
            : brandRepository.findById(id(id)).orElseThrow(ApiException::missing);
    if ((id == null || !brand.getName().equalsIgnoreCase(data.name().strip()))
        && brandRepository.existsByNameIgnoreCase(data.name().strip()))
      throw ApiException.conflict("Marca já cadastrada");
    brand.setName(data.name().strip());
    brandRepository.save(brand);
    auditService.record(id == null ? "CREATE" : "UPDATE", "Brand", brand.getId());
    return new Named(brand.getId().toString(), brand.getName());
  }

  public Named category(String id, NameInput data) {
    var category =
        id == null
            ? new Category()
            : categoryRepository.findById(id(id)).orElseThrow(ApiException::missing);
    if ((id == null || !category.getName().equalsIgnoreCase(data.name().strip()))
        && categoryRepository.existsByNameIgnoreCase(data.name().strip()))
      throw ApiException.conflict("Categoria já cadastrada");
    category.setName(data.name().strip());
    categoryRepository.save(category);
    auditService.record(id == null ? "CREATE" : "UPDATE", "Category", category.getId());
    return new Named(category.getId().toString(), category.getName());
  }

  public void deleteBrand(String id) {
    var brand = brandRepository.findById(id(id)).orElseThrow(ApiException::missing);
    auditService.record("DELETE", "Brand", brand.getId());
    brandRepository.delete(brand);
  }

  public void deleteCategory(String id) {
    var category = categoryRepository.findById(id(id)).orElseThrow(ApiException::missing);
    auditService.record("DELETE", "Category", category.getId());
    categoryRepository.delete(category);
  }

  @Transactional(readOnly = true)
  public PublicProduct detail(String id) {
    return publicView(
        productRepository
            .findById(id(id))
            .filter(Product::getActive)
            .orElseThrow(ApiException::missing));
  }
}
