package br.com.loja.service;

import br.com.loja.dto.CampaignDtos.*;
import br.com.loja.dto.CatalogDtos.PageView;
import br.com.loja.exception.ApiException;
import br.com.loja.model.*;
import br.com.loja.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CampaignService {
  private final CouponRepository couponRepository;
  private final PromotionRepository promotionRepository;
  private final ProductPromotionRepository productPromotionRepository;
  private final ProductRepository productRepository;
  private final InterestRateRepository interestRateRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final AuditService auditService;

  public CouponView view(Coupon coupon) {
    return new CouponView(
        coupon.getId().toString(),
        coupon.getCode(),
        coupon.getType(),
        coupon.getValue(),
        coupon.getValidFrom(),
        coupon.getValidUntil(),
        coupon.getUsageLimit(),
        purchaseOrderRepository.countByCouponIdAndStatusNot(coupon.getId(), "CANCELLED"));
  }

  public PromotionView view(Promotion promotion) {
    return new PromotionView(
        promotion.getId().toString(),
        promotion.getName(),
        promotion.getPercent(),
        promotion.getValidFrom(),
        promotion.getValidUntil(),
        productPromotionRepository.findByPromotionId(promotion.getId()).stream()
            .map(productPromotion -> productPromotion.getProduct().getId().toString())
            .toList());
  }

  @Transactional(readOnly = true)
  public PageView<CouponView> coupons(int page, int size) {
    return PageView.from(couponRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  @Transactional(readOnly = true)
  public PageView<PromotionView> promotions(int page, int size) {
    return PageView.from(
        promotionRepository.findAll(CatalogService.page(page, size)).map(this::view));
  }

  public CouponView coupon(String id, CouponInput couponInput) {
    if (couponInput.validUntil().isBefore(couponInput.validFrom())
        || ("PERCENT".equals(couponInput.type())
            && couponInput.value().compareTo(new java.math.BigDecimal("100")) > 0))
      throw new ApiException(400, "Período ou percentual inválido");
    var coupon =
        id == null
            ? new Coupon()
            : couponRepository.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    coupon.setCode(couponInput.code().strip().toUpperCase(Locale.ROOT));
    coupon.setType(couponInput.type());
    coupon.setValue(couponInput.value());
    coupon.setValidFrom(couponInput.validFrom());
    coupon.setValidUntil(couponInput.validUntil());
    coupon.setUsageLimit(couponInput.usageLimit());
    couponRepository.save(coupon);
    auditService.record(id == null ? "CREATE" : "UPDATE", "Coupon", coupon.getId());
    return view(coupon);
  }

  public void deleteCoupon(String id) {
    var coupon =
        couponRepository.lockById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    auditService.record("DELETE", "Coupon", coupon.getId());
    couponRepository.delete(coupon);
  }

  public PromotionView promotion(String id, PromotionInput promotionInput) {
    if (promotionInput.validUntil().isBefore(promotionInput.validFrom()))
      throw new ApiException(400, "Período inválido");
    var promotion =
        id == null
            ? new Promotion()
            : promotionRepository
                .findById(CatalogService.id(id))
                .orElseThrow(ApiException::missing);
    promotion.setName(promotionInput.name().strip());
    promotion.setPercent(promotionInput.percent());
    promotion.setValidFrom(promotionInput.validFrom());
    promotion.setValidUntil(promotionInput.validUntil());
    promotionRepository.save(promotion);
    if (id != null) {
      productPromotionRepository.deleteAll(
          productPromotionRepository.findByPromotionId(promotion.getId()));
      productPromotionRepository.flush();
    }
    for (String productId : new LinkedHashSet<>(promotionInput.productIds())) {
      var productPromotion = new ProductPromotion();
      productPromotion.setProduct(
          productRepository
              .findById(CatalogService.id(productId))
              .orElseThrow(ApiException::missing));
      productPromotion.setPromotion(promotion);
      productPromotionRepository.save(productPromotion);
    }
    auditService.record(id == null ? "CREATE" : "UPDATE", "Promotion", promotion.getId());
    return view(promotion);
  }

  public void deletePromotion(String id) {
    var promotion =
        promotionRepository.findById(CatalogService.id(id)).orElseThrow(ApiException::missing);
    productPromotionRepository.deleteAll(
        productPromotionRepository.findByPromotionId(promotion.getId()));
    auditService.record("DELETE", "Promotion", promotion.getId());
    promotionRepository.delete(promotion);
  }

  @Transactional(readOnly = true)
  public List<RateView> rates() {
    return interestRateRepository.findAll(Sort.by("installmentCount")).stream()
        .map(
            interestRate ->
                new RateView(
                    interestRate.getId().toString(),
                    interestRate.getInstallmentCount(),
                    interestRate.getPercent()))
        .toList();
  }

  public RateView rate(RateInput rateInput) {
    var interestRate =
        interestRateRepository
            .findByInstallmentCount(rateInput.installmentCount())
            .orElseGet(InterestRate::new);
    interestRate.setInstallmentCount(rateInput.installmentCount());
    interestRate.setPercent(rateInput.percent());
    interestRateRepository.save(interestRate);
    auditService.record("UPDATE", "InterestRate", interestRate.getId());
    return new RateView(
        interestRate.getId().toString(),
        interestRate.getInstallmentCount(),
        interestRate.getPercent());
  }
}
