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
@Constraint(validatedBy = Utf8ByteSizeValidator.class)
public @interface Utf8ByteSize {
  String message() default "deve ter no máximo 72 bytes UTF-8";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int maximum() default 72;
}
