package com.vovan4ok.appliance.store.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String SPECIAL_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true;
        }

        return password.length() >= MIN_LENGTH
                && password.matches(UPPERCASE_PATTERN)
                && password.matches(LOWERCASE_PATTERN)
                && password.matches(SPECIAL_PATTERN);
    }
}
