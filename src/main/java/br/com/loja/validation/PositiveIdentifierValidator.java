package br.com.loja.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PositiveIdentifierValidator
    implements ConstraintValidator<PositiveIdentifier, String> {
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true;
    if (value.length() > 19 || !value.matches("[1-9][0-9]*")) return false;
    try {
      return Long.parseLong(value) > 0;
    } catch (NumberFormatException exception) {
      return false;
    }
  }
}
