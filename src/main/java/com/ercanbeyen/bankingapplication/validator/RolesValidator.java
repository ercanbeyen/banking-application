package com.ercanbeyen.bankingapplication.validator;

import com.ercanbeyen.bankingapplication.annotation.RolesRequest;
import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;
import java.util.Set;

public class RolesValidator implements ConstraintValidator<RolesRequest, Set<String>> {
    @Override
    public void initialize(RolesRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Set<String> roles, ConstraintValidatorContext constraintValidatorContext) {
        if (Optional.ofNullable(roles).isEmpty() || roles.isEmpty()) {
            return false;
        }

        for (String role : roles) {
            try {
                ERole.valueOf(role);
            } catch (IllegalArgumentException _) {
                return false;
            }
        }

        return true;
    }
}
