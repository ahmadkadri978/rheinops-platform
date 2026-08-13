package dev.rheinops.platform.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRateLimitFilterTest {

    @Test
    void rejectsRequestsAfterTheClientBucketIsExhausted() throws Exception {
        ApiRateLimitProperties properties = new ApiRateLimitProperties(
                true, 1, 1, Duration.ofHours(1), 100, Duration.ofMinutes(10));
        ApiProblemWriter writer = new ApiProblemWriter(
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC));
        ApiRateLimitFilter filter = new ApiRateLimitFilter(properties, writer);

        MockHttpServletRequest firstRequest = apiRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = apiRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
        assertThat(secondResponse.getContentAsString()).contains("rate_limit_exceeded");
    }

    private static MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/incidents");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
