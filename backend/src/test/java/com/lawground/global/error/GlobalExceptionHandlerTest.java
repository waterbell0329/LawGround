package com.lawground.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lawground.global.web.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var fixedClock = Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneOffset.UTC);
        mvc =
                MockMvcBuilders.standaloneSetup(new ProbeController())
                        .setControllerAdvice(new GlobalExceptionHandler(fixedClock))
                        .addFilters(new RequestIdFilter())
                        .build();
    }

    @Test
    void validationReturnsStructured400WithoutRejectedValue() throws Exception {
        mvc.perform(
                        post("/probe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}")
                                .header(RequestIdFilter.HEADER, "validation-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value("validation-1"))
                .andExpect(jsonPath("$.timestamp").value("2026-09-28T00:00:00Z"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalMessage() throws Exception {
        mvc.perform(post("/probe/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("요청을 처리하지 못했습니다."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class ProbeController {
        @PostMapping("/probe")
        String validate(@Valid @RequestBody ProbeDTO body) {
            return body.name();
        }

        @PostMapping("/probe/failure")
        void fail() {
            throw new IllegalStateException("sensitive internal message");
        }
    }

    record ProbeDTO(@NotBlank String name) {}
}
