package com.lawground.global.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = SecurityConfigurationTest.CsrfProbeController.class,
        properties = "lawground.security.allow-local-docs=true")
@Import({
    SecurityConfiguration.class,
    ClockConfiguration.class,
    SecurityConfigurationTest.CsrfProbeController.class
})
@ActiveProfiles("test")
class SecurityConfigurationTest {
    @Autowired private MockMvc mvc;

    @Test
    void privateApiRequiresAuthenticationAndReturnsRequestId() throws Exception {
        mvc.perform(get("/api/v1/private-probe").header("X-Request-Id", "security-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(header().string("X-Request-Id", "security-1"));
    }

    @Test
    void authenticatedUserStillNeedsExplicitApiPermission() throws Exception {
        mvc.perform(get("/api/v1/private-probe").with(user("probe")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void csrfProtectionRemainsEnabled() throws Exception {
        mvc.perform(post("/actuator/health/csrf-probe").with(user("probe")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void publicPostAcceptsValidCsrfToken() throws Exception {
        mvc.perform(post("/actuator/health/csrf-probe").with(csrf())).andExpect(status().isOk());
    }

    @RestController
    static class CsrfProbeController {
        @PostMapping("/actuator/health/csrf-probe")
        String post() {
            return "ok";
        }
    }

    @Test
    void localDocumentationPermissionPassesSecurityEvenWithoutMvcRoute() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
