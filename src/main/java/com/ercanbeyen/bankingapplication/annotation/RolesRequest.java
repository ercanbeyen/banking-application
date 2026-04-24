package com.ercanbeyen.bankingapplication.annotation;

import com.ercanbeyen.bankingapplication.validator.RolesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RolesValidator.class)
public @interface RolesRequest {
    String message() default "Invalid roles";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
