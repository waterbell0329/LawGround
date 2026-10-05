package com.lawground.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiInputFilterTest {
    @Test
    void unknownLengthIsBoundedAndAllowedBodyIsPreserved() throws Exception {
        for (int length :
                new int[] {ApiInputFilter.MAX_BODY_BYTES, ApiInputFilter.MAX_BODY_BYTES + 1}) {
            var request =
                    new MockHttpServletRequest("POST", "/api/v1/questions") {
                        @Override
                        public long getContentLengthLong() {
                            return -1;
                        }

                        @Override
                        public int getContentLength() {
                            return -1;
                        }
                    };
            request.setContent(new byte[length]);
            var response = new MockHttpServletResponse();
            var downstream = new AtomicInteger();
            var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
            new ApiInputFilter(mapper, Clock.systemUTC())
                    .doFilter(
                            request,
                            response,
                            (req, res) -> {
                                downstream.incrementAndGet();
                                assertThat(req.getInputStream().readAllBytes()).hasSize(length);
                            });
            assertThat(downstream.get()).isEqualTo(length > ApiInputFilter.MAX_BODY_BYTES ? 0 : 1);
            assertThat(response.getStatus())
                    .isEqualTo(length > ApiInputFilter.MAX_BODY_BYTES ? 413 : 200);
        }
    }
}
