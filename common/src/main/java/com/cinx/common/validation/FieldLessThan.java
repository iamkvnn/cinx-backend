package com.cinx.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = FieldLessThanValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldLessThan {
    String message() default "Discounted price must be less than price";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String smallerField() default "discountedPrice";
    String largerField() default "price";
}
