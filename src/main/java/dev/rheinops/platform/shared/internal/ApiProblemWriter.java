package dev.rheinops.platform.shared.internal;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

final class ApiProblemWriter {

    private final Clock clock;

    ApiProblemWriter(Clock clock) {
        this.clock = clock;
    }

    void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write("""
                        {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s","timestamp":"%s","correlationId":"%s"}
                        """
                        .formatted(
                                escape(status.getReasonPhrase()),
                                status.value(),
                                escape(detail),
                                escape(code),
                                clock.instant(),
                                escape(correlationId()))
                        .trim());
    }

    private static String correlationId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId == null ? "unavailable" : correlationId;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
