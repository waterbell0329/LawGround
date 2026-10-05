package com.lawground.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointTextValidator implements ConstraintValidator<CodePointText, String> {
    private int max;

    @Override
    public void initialize(CodePointText constraint) {
        max = constraint.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return TextPolicy.valid(value, max);
    }
}
