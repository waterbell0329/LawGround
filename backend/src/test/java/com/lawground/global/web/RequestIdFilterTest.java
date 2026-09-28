package com.lawground.global.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void preservesSafeCallerIdAndClearsLoggingContext() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER, "client-123");
        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    assertThat(req.getAttribute(RequestIdFilter.ATTRIBUTE)).isEqualTo("client-123");
                    assertThat(MDC.get(RequestIdFilter.ATTRIBUTE)).isEqualTo("client-123");
                });
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-123");
        assertThat(MDC.get(RequestIdFilter.ATTRIBUTE)).isNull();
    }

    @Test
    void replacesUnsafeCallerId() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER, "invalid id with spaces");
        filter.doFilter(request, response, (req, res) -> {});
        assertThat(UUID.fromString(response.getHeader(RequestIdFilter.HEADER))).isNotNull();
        assertThat(MDC.get(RequestIdFilter.ATTRIBUTE)).isNull();
    }

    @Test
    void clearsLoggingContextEvenIfDownstreamFails() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        request,
                                        response,
                                        (req, res) -> {
                                            throw new IllegalStateException("test failure");
                                        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("test failure");
        assertThat(UUID.fromString(response.getHeader(RequestIdFilter.HEADER))).isNotNull();
        assertThat(MDC.get(RequestIdFilter.ATTRIBUTE)).isNull();
    }
}
