package br.com.florir.controller;

import br.com.florir.dto.CampaignDtos.*;
import br.com.florir.dto.CatalogDtos.PageView;
import br.com.florir.service.CampaignService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager")
public class CampaignController {
  private final CampaignService service;

  public CampaignController(CampaignService s) {
    service = s;
  }

  @GetMapping("/coupons")
  PageView<CouponView> coupons(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.coupons(page, size);
  }

  @PostMapping("/coupons")
  @ResponseStatus(HttpStatus.CREATED)
  CouponView createCoupon(@Valid @RequestBody CouponInput d) {
    return service.coupon(null, d);
  }

  @PutMapping("/coupons/{id}")
  CouponView updateCoupon(@PathVariable String id, @Valid @RequestBody CouponInput d) {
    return service.coupon(id, d);
  }

  @DeleteMapping("/coupons/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteCoupon(@PathVariable String id) {
    service.deleteCoupon(id);
  }

  @GetMapping("/promotions")
  PageView<PromotionView> promotions(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.promotions(page, size);
  }

  @PostMapping("/promotions")
  @ResponseStatus(HttpStatus.CREATED)
  PromotionView createPromotion(@Valid @RequestBody PromotionInput d) {
    return service.promotion(null, d);
  }

  @PutMapping("/promotions/{id}")
  PromotionView updatePromotion(@PathVariable String id, @Valid @RequestBody PromotionInput d) {
    return service.promotion(id, d);
  }

  @DeleteMapping("/promotions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deletePromotion(@PathVariable String id) {
    service.deletePromotion(id);
  }

  @GetMapping("/interest-rates")
  List<RateView> rates() {
    return service.rates();
  }

  @PutMapping("/interest-rates")
  RateView rate(@Valid @RequestBody RateInput d) {
    return service.rate(d);
  }
}
