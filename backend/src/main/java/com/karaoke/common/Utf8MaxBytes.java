package com.karaoke.common;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Limite la taille d'une chaîne en octets UTF-8. Sert pour les mots de passe : BCrypt n'utilise
 * que les 72 premiers octets, et un caractère accentué en occupe deux.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Utf8MaxBytes.Validator.class)
public @interface Utf8MaxBytes {

    int value();

    String message() default "est trop long (maximum {value} octets)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<Utf8MaxBytes, String> {

        private int max;

        @Override
        public void initialize(Utf8MaxBytes annotation) {
            this.max = annotation.value();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
        }
    }
}
