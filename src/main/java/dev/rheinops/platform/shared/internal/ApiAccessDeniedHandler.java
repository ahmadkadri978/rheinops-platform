package dev.rheinops.platform.shared.internal;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

final class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiProblemWriter problemWriter;

    ApiAccessDeniedHandler(ApiProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException, ServletException {
        problemWriter.write(
                response,
                HttpStatus.FORBIDDEN,
                "insufficient_permissions",
                "The authenticated actor is not allowed to perform this operation.");
    }
}
