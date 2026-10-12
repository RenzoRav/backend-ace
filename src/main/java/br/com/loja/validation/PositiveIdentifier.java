package br.com.loja.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.RECORD_COMPONENT,
  ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PositiveIdentifierValidator.class)
public @interface PositiveIdentifier {
  String message() default "deve ser um identificador positivo válido";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
