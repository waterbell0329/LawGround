package com.lawground.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class LocalDevelopmentAuthTest {
    @Test
    void requiresExplicitLocalLoopbackAndBlocksMixedProdTestProfiles() {
        for (String profile : new String[] {"prod", "test"}) {
            var env = new MockEnvironment();
            env.setActiveProfiles(profile);
            assertThatThrownBy(() -> LocalDevelopmentAuth.validate(true, env, "127.0.0.1"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatCode(() -> LocalDevelopmentAuth.validate(false, env, "0.0.0.0"))
                    .doesNotThrowAnyException();
            env.setActiveProfiles("local", profile);
            assertThatThrownBy(() -> LocalDevelopmentAuth.validate(true, env, "127.0.0.1"))
                    .isInstanceOf(IllegalStateException.class);
        }
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertThatThrownBy(() -> LocalDevelopmentAuth.validate(true, local, "0.0.0.0"))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> LocalDevelopmentAuth.validate(true, local, "127.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void clientHeaderDoesNotSelectDevelopmentPrincipal() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Member-Id", "forged");
        try {
            new LocalDevelopmentAuth()
                    .doFilter(
                            request,
                            new MockHttpServletResponse(),
                            (req, res) ->
                                    assertThat(
                                                    SecurityContextHolder.getContext()
                                                            .getAuthentication()
                                                            .getName())
                                            .isEqualTo(LocalDevelopmentAuth.MEMBER_ID.toString()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
