package com.lawground.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class LocalDevelopmentAuth extends OncePerRequestFilter {
    public static final UUID MEMBER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    public static void validate(boolean enabled, Environment environment, String address) {
        if (enabled
                && (!environment.matchesProfiles("local & !prod & !test")
                        || !List.of("127.0.0.1", "::1").contains(address))) {
            throw new IllegalStateException(
                    "Development authentication requires local-only loopback configuration");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            MEMBER_ID.toString(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
