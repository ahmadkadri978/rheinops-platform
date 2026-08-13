package dev.rheinops.platform.shared.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class HttpAccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAccessLogFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object matchedPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = matchedPattern instanceof String pattern ? pattern : "unmatched";
            LOGGER.atInfo()
                    .addKeyValue("event.action", "http_request_completed")
                    .addKeyValue("event.duration", System.nanoTime() - startedAt)
                    .addKeyValue("http.request.method", request.getMethod())
                    .addKeyValue("http.response.status_code", response.getStatus())
                    .addKeyValue("url.route", route)
                    .log("HTTP request completed");
        }
    }
}
