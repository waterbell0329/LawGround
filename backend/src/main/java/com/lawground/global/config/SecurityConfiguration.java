package com.lawground.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawground.global.error.ErrorCode;
import com.lawground.global.error.ErrorResponseDTO;
import com.lawground.global.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper mapper,
            Clock clock,
            @Value("${lawground.security.allow-local-docs:false}") boolean allowLocalDocs)
            throws Exception {
        http.authorizeHttpRequests(
                requests -> {
                    requests.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (allowLocalDocs) {
                        requests.requestMatchers(
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**")
                                .permitAll();
                    }
                    // Y04/Y11 will add explicit API permissions and authentication.
                    requests.anyRequest().denyAll();
                });
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        // Keep CSRF protection enabled for future session-based browser requests.
        http.exceptionHandling(
                errors ->
                        errors.authenticationEntryPoint(
                                        (request, response, exception) ->
                                                writeError(
                                                        request,
                                                        response,
                                                        mapper,
                                                        clock,
                                                        401,
                                                        ErrorCode.AUTHENTICATION_REQUIRED,
                                                        "인증이 필요합니다."))
                                .accessDeniedHandler(
                                        (request, response, exception) ->
                                                writeError(
                                                        request,
                                                        response,
                                                        mapper,
                                                        clock,
                                                        403,
                                                        ErrorCode.ACCESS_DENIED,
                                                        "접근할 수 없습니다.")));
        return http.build();
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            ObjectMapper mapper,
            Clock clock,
            int status,
            ErrorCode code,
            String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(
                response.getWriter(),
                new ErrorResponseDTO(
                        code,
                        message,
                        (String) request.getAttribute(RequestIdFilter.ATTRIBUTE),
                        Instant.now(clock),
                        List.of()));
    }
}
