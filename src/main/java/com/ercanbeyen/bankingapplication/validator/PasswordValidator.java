package com.ercanbeyen.bankingapplication.validator;

import com.ercanbeyen.bankingapplication.annotation.PasswordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PasswordValidator implements ConstraintValidator<PasswordRequest, String> {
    private static final int PASSWORD_LENGTH = 6;
    private static final int ALLOWED_MAX_REPEATING_DIGITS = 1;
    private static final int ALLOWED_MAX_CONSECUTIVE_DIGITS = 2;

    @Override
    public void initialize(PasswordRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        if (password.length() != PASSWORD_LENGTH) {
            return false;
        }

        try {
            Integer.parseInt(password);
        } catch (NumberFormatException _) {
            return false;
        }

        return !hasRepeatingDigits(password) && !hasConsecutiveDigits(password);
    }

    private static boolean hasRepeatingDigits(String password) {
        Set<Character> passwordCharacters = password.chars()
                .mapToObj(character -> (char) character)
                .collect(Collectors.toSet());

        return passwordCharacters.size() < PASSWORD_LENGTH - ALLOWED_MAX_REPEATING_DIGITS;
    }

    private static boolean hasConsecutiveDigits(String password) {
        List<Integer> digits = password.chars()
                .map(Character::getNumericValue)
                .boxed()
                .toList();

        int ascendingCount = 1;
        int descendingCount = 1;
        boolean hasConsecutiveDigits = false;

        for (int i = 1; i < digits.size() && !hasConsecutiveDigits; i++) {
            if (digits.get(i) == digits.get(i - 1) + 1) { // ascending digits case
                ascendingCount++;
            } else {
                ascendingCount = 1;
            }

            if (digits.get(i) == digits.get(i - 1) - 1) { // descending digits case
                descendingCount++;
            } else {
                descendingCount = 1;
            }

            if (ascendingCount > ALLOWED_MAX_CONSECUTIVE_DIGITS || descendingCount > ALLOWED_MAX_CONSECUTIVE_DIGITS) {
                hasConsecutiveDigits = true;
            }
        }

        return hasConsecutiveDigits;
    }
}
