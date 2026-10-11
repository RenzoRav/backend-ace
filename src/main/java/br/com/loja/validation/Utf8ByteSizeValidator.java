package br.com.loja.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
public class Utf8ByteSizeValidator implements ConstraintValidator<Utf8ByteSize,String> {
 private int maximum;
 public void initialize(Utf8ByteSize constraint){maximum=constraint.maximum();}
 public boolean isValid(String value,ConstraintValidatorContext context){return value==null || (value.length()<=maximum && value.getBytes(StandardCharsets.UTF_8).length<=maximum);}
}
