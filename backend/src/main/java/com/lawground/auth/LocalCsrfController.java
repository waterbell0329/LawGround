package com.lawground.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local & !prod & !test")
@ConditionalOnProperty(name = "lawground.security.dev-auth-enabled", havingValue = "true")
public class LocalCsrfController {
    @GetMapping("/api/v1/dev/csrf")
    public Token token(CsrfToken csrf) {
        return new Token(csrf.getToken(), csrf.getHeaderName(), csrf.getParameterName());
    }

    public record Token(String token, String headerName, String parameterName) {}
}
