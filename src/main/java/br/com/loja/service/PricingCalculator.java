package br.com.loja.service;

import java.math.*;
import java.util.*;

public final class PricingCalculator {
  private PricingCalculator() {}

  public record Result(
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal shipping,
      BigDecimal interest,
      BigDecimal total) {}

  public static BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  public static Result calculate(
      BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal percent) {
    if (subtotal.signum() < 0
        || discount.signum() < 0
        || shipping.signum() < 0
        || percent.signum() < 0) throw new IllegalArgumentException("Valores negativos");
    discount = money(discount.min(subtotal));
    var base = money(subtotal.subtract(discount).add(shipping));
    var interest = money(base.multiply(percent).divide(new BigDecimal("100")));
    return new Result(
        money(subtotal), discount, money(shipping), interest, money(base.add(interest)));
  }

  public static List<BigDecimal> split(BigDecimal total, int count) {
    if (count < 1 || count > 12)
      throw new IllegalArgumentException("Quantidade de parcelas entre 1 e 12");
    long cents = money(total).movePointRight(2).longValueExact();
    if (cents < count)
      throw new IllegalArgumentException("Cada parcela precisa ter pelo menos um centavo");
    var amounts = new ArrayList<BigDecimal>();
    for (int installmentIndex = 0; installmentIndex < count; installmentIndex++)
      amounts.add(
          BigDecimal.valueOf(cents / count + (installmentIndex < cents % count ? 1 : 0), 2));
    return List.copyOf(amounts);
  }
}
