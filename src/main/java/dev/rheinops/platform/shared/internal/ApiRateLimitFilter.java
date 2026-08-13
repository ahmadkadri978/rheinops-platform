package dev.rheinops.platform.shared.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

final class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final ApiRateLimitProperties properties;
    private final ApiProblemWriter problemWriter;
    private final Cache<String, Bucket> buckets;

    ApiRateLimitFilter(ApiRateLimitProperties properties, ApiProblemWriter problemWriter) {
        this.properties = properties;
        this.problemWriter = problemWriter;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxClients())
                .expireAfterAccess(properties.expireAfterAccess())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Bucket bucket = buckets.get(request.getRemoteAddr(), ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(LIMIT_HEADER, Long.toString(properties.capacity()));
        response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        problemWriter.write(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "rate_limit_exceeded",
                "Too many API requests. Retry after %d seconds.".formatted(retryAfterSeconds));
    }

    private Bucket newBucket() {
        Duration refillPeriod = properties.refillPeriod();
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(properties.capacity())
                        .refillGreedy(properties.refillTokens(), refillPeriod))
                .build();
    }
}
