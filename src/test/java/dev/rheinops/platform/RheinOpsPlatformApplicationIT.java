package dev.rheinops.platform;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.rheinops.platform.shared.PlatformRole;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RheinOpsPlatformApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void managesAnIncidentLifecycleAndRejectsStaleUpdates() throws Exception {
        String serviceResponse = mockMvc.perform(post("/api/v1/services")
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Customer Portal",
                                  "slug": "customer-portal",
                                  "description": "Customer-facing account portal"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status", is("OPERATIONAL")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String serviceId = JsonPath.read(serviceResponse, "$.id");

        String incidentResponse = mockMvc.perform(post("/api/v1/incidents")
                        .with(asRole(
                                PlatformRole.RESPONDER,
                                "responder-subject",
                                "responder"))
                        .header("X-Correlation-ID", "portfolio-it-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Login requests are timing out",
                                  "description": "Customers receive a timeout during login.",
                                  "severity": "SEV2",
                                  "affectedServiceId": "%s"
                                }
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", "portfolio-it-001"))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String incidentId = JsonPath.read(incidentResponse, "$.id");

        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .with(asRole(
                                PlatformRole.RESPONDER,
                                "responder-subject",
                                "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INVESTIGATING", "expectedVersion": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INVESTIGATING")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .with(asRole(
                                PlatformRole.RESPONDER,
                                "responder-subject",
                                "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IDENTIFIED", "expectedVersion": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("stale_incident_version")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));

        mockMvc.perform(get("/api/v1/incidents")
                        .with(asRole(PlatformRole.VIEWER, "viewer-subject", "viewer"))
                        .queryParam("status", "INVESTIGATING")
                        .queryParam("severity", "SEV2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(incidentId)));

        mockMvc.perform(get("/api/v1/incidents/{incidentId}/timeline", incidentId)
                        .with(asRole(PlatformRole.VIEWER, "viewer-subject", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].eventType", is("INCIDENT_CREATED")))
                .andExpect(jsonPath("$.content[0].previousStatus").doesNotExist())
                .andExpect(jsonPath("$.content[0].newStatus", is("OPEN")))
                .andExpect(jsonPath("$.content[0].actor.subject", is("responder-subject")))
                .andExpect(jsonPath("$.content[0].actor.displayName", is("responder")))
                .andExpect(jsonPath("$.content[1].eventType", is("STATUS_CHANGED")))
                .andExpect(jsonPath("$.content[1].previousStatus", is("OPEN")))
                .andExpect(jsonPath("$.content[1].newStatus", is("INVESTIGATING")));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE incident_timeline_events SET actor_display_name = ? WHERE incident_id = ?::uuid",
                        "tampered",
                        incidentId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void returnsProblemDetailsForInvalidRequests() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "slug": "INVALID SLUG"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("validation_failed")))
                .andExpect(jsonPath("$.violations", notNullValue()));
    }

    private static RequestPostProcessor asRole(
            PlatformRole role, String subject, String preferredUsername) {
        return jwt()
                .jwt(token -> token.subject(subject).claim("preferred_username", preferredUsername))
                .authorities(new SimpleGrantedAuthority(role.authority()));
    }
}
