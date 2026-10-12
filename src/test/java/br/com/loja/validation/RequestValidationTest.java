package br.com.loja.validation;

import static org.junit.jupiter.api.Assertions.*;

import br.com.loja.dto.AuthDtos;
import br.com.loja.dto.CustomerDtos;
import br.com.loja.dto.OrderDtos;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RequestValidationTest {
  final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsOversizedLoginAndUtf8Password() {
    assertFalse(
        validator
            .validate(new AuthDtos.Login("x".repeat(256) + "@example.test", "ValidPassword123!"))
            .isEmpty());
    assertFalse(
        validator.validate(new AuthDtos.Login("test@example.test", "á".repeat(37))).isEmpty());
    assertTrue(
        validator.validate(new AuthDtos.Login("test@example.test", "á".repeat(36))).isEmpty());
  }

  @Test
  void rejectsFutureBirthdayAndLongPhone() {
    assertFalse(
        validator
            .validate(
                new CustomerDtos.CustomerInput("Name", "1".repeat(33), LocalDate.now().plusDays(1)))
            .isEmpty());
  }

  @Test
  void rejectsOversizedAddress() {
    assertFalse(
        validator
            .validate(
                new CustomerDtos.AddressInput(
                    "a".repeat(256), "12345678", "street", "1", null, "district", "city", "SP"))
            .isEmpty());
  }

  @Test
  void rejectsInvalidIdentifiersAndNullItems() {
    assertFalse(validator.validate(new OrderDtos.ItemInput("-1", 1)).isEmpty());
    assertFalse(
        validator.validate(new OrderDtos.ItemInput("999999999999999999999999", 1)).isEmpty());
    assertFalse(
        validator
            .validate(
                new OrderDtos.OrderInput(
                    null, "PIX", 1, false, null, null, Arrays.asList((OrderDtos.ItemInput) null)))
            .isEmpty());
  }
}
