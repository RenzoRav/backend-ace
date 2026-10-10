package br.com.florir.controller;

import br.com.florir.dto.CampaignDtos.*;
import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.service.CampaignService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class CampaignController {
  private final CampaignService campaignService;

  @GetMapping("/coupons")
  PageView<CouponView> coupons(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return campaignService.coupons(page, size);
  }

  @PostMapping("/coupons")
  @ResponseStatus(HttpStatus.CREATED)
  CouponView createCoupon(@Valid @RequestBody CouponInput couponInput) {
    return campaignService.coupon(null, couponInput);
  }

  @PutMapping("/coupons/{id}")
  CouponView updateCoupon(@PathVariable String id, @Valid @RequestBody CouponInput couponInput) {
    return campaignService.coupon(id, couponInput);
  }

  @DeleteMapping("/coupons/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteCoupon(@PathVariable String id) {
    campaignService.deleteCoupon(id);
  }

  @GetMapping("/promotions")
  PageView<PromotionView> promotions(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return campaignService.promotions(page, size);
  }

  @PostMapping("/promotions")
  @ResponseStatus(HttpStatus.CREATED)
  PromotionView createPromotion(@Valid @RequestBody PromotionInput promotionInput) {
    return campaignService.promotion(null, promotionInput);
  }

  @PutMapping("/promotions/{id}")
  PromotionView updatePromotion(
      @PathVariable String id, @Valid @RequestBody PromotionInput promotionInput) {
    return campaignService.promotion(id, promotionInput);
  }

  @DeleteMapping("/promotions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deletePromotion(@PathVariable String id) {
    campaignService.deletePromotion(id);
  }

  @GetMapping("/interest-rates")
  List<RateView> rates() {
    return campaignService.rates();
  }

  @PutMapping("/interest-rates")
  RateView rate(@Valid @RequestBody RateInput rateInput) {
    return campaignService.rate(rateInput);
  }
}
