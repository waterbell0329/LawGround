package com.lawground.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodePointTextValidator.class)
public @interface CodePointText {
    String message() default "공백이 아닌 허용 길이의 문자열을 입력하세요.";

    int max();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
