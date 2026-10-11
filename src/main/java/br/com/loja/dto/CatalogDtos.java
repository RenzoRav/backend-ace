package br.com.loja.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class CatalogDtos {
  public record NameInput(@NotBlank @Size(max = 255) String name) {}

  public record Named(String id, String name) {}

  public record ProductInput(
      @NotBlank @Size(max = 255) String name,
      @Size(max = 4000) String description,
      @Size(max = 4000) String imageUrl,
      @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal price,
      @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal cost,
      @Min(0) int stock,
      @Min(0) int minimumStock,
      @NotBlank String brandId,
      @NotBlank String categoryId,
      boolean active) {}

  public record ProductView(
      String id,
      String name,
      String description,
      String imageUrl,
      BigDecimal price,
      BigDecimal cost,
      int stock,
      int minimumStock,
      boolean active,
      Named brand,
      Named category) {}

  public record PublicProduct(
      String id,
      String name,
      String description,
      String imageUrl,
      BigDecimal price,
      int stock,
      Named brand,
      Named category) {}

  public record PageView<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageView<T> from(org.springframework.data.domain.Page<T> page) {
      return new PageView<>(
          page.getContent(),
          page.getNumber(),
          page.getSize(),
          page.getTotalElements(),
          page.getTotalPages());
    }
  }

  public record StockAdjustment(@Min(0) int stock, @NotBlank @Size(max = 4000) String reason) {}
}
