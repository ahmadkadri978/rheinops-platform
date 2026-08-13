package dev.rheinops.platform.support.web;

import dev.rheinops.platform.shared.PageResponse;
import dev.rheinops.platform.support.OperationalReadOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/operations")
@Tag(name = "Operational Read Models")
@SecurityRequirement(name = "bearerAuth")
public class OperationalReadController {

    private final OperationalReadOperations operations;

    public OperationalReadController(OperationalReadOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across incidents and support diagnostics")
    PageResponse<OperationalSearchResponse> search(
            @RequestParam @NotBlank @Size(min = 2, max = 200) String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(operations.search(query, pageable), OperationalSearchResponse::from);
    }

    @GetMapping(value = "/exports/cases", produces = "text/csv")
    @Operation(summary = "Export a redacted operational case report")
    ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        var export = operations.export(from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(export.filename()).build());
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(export.content());
    }
}
