package br.com.loja.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {
  @Test
  void calculatesApprovedExample() {
    var result =
        PricingCalculator.calculate(
            new BigDecimal("180"), new BigDecimal("18"), new BigDecimal("12"), new BigDecimal("5"));
    assertEquals(new BigDecimal("182.70"), result.total());
    assertEquals(new BigDecimal("8.70"), result.interest());
    assertEquals(
        java.util.List.of(
            new BigDecimal("60.90"), new BigDecimal("60.90"), new BigDecimal("60.90")),
        PricingCalculator.split(result.total(), 3));
  }

  @Test
  void distributesRemainderExactly() {
    assertEquals(
        new BigDecimal("10.00"),
        PricingCalculator.split(new BigDecimal("10"), 3).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  @Test
  void rejectsZeroValueInstallments() {
    assertThrows(
        IllegalArgumentException.class, () -> PricingCalculator.split(new BigDecimal("0.01"), 2));
  }

  @Test
  void normalizesPhonesWithoutConfusingDdd55() {
    assertEquals("55912345678", PhoneNumbers.normalize("(55) 91234-5678"));
    assertEquals("11912345678", PhoneNumbers.normalize("+55 (11) 91234-5678"));
  }
}
