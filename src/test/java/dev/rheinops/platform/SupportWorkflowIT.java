package dev.rheinops.platform;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.rheinops.platform.shared.PlatformRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SupportWorkflowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void managesSupportSlaSearchAndRedactedExport() throws Exception {
        String serviceId = createService();
        String organisationId = createOrganisation();
        createSlaPolicy(organisationId);
        String incidentId = createIncident(serviceId);
        String supportCaseResponse = createSupportCase(organisationId, serviceId, incidentId);
        String supportCaseId = JsonPath.read(supportCaseResponse, "$.id");
        String caseReference = JsonPath.read(supportCaseResponse, "$.caseReference");

        String overdueCaseResponse = createSupportCase(organisationId, serviceId, incidentId);
        String overdueCaseId = JsonPath.read(overdueCaseResponse, "$.id");
        jdbcTemplate.update("""
                UPDATE support_cases
                SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                    updated_at = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                    response_due_at = CURRENT_TIMESTAMP - INTERVAL '1 hour',
                    resolution_due_at = CURRENT_TIMESTAMP + INTERVAL '1 hour'
                WHERE id = ?::uuid
                """, overdueCaseId);

        mockMvc.perform(get("/api/v1/support-cases/{supportCaseId}", overdueCaseId)
                        .with(asRole(PlatformRole.VIEWER, "viewer-subject", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sla.health", is("BREACHED")))
                .andExpect(jsonPath("$.escalationLevel", is("BREACHED")))
                .andExpect(jsonPath("$.version", is(1)));

        String diagnosticResponse = mockMvc.perform(post(
                                "/api/v1/support-cases/{supportCaseId}/diagnostics", supportCaseId)
                        .with(asRole(PlatformRole.RESPONDER, "responder-subject", "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LOG_EXCERPT",
                                  "summary": "Quasarflux latency confirmed",
                                  "details": "SECRET-DIAGNOSTIC: quasarflux saturation appears in the gateway log"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actor.subject", is("responder-subject")))
                .andReturn().getResponse().getContentAsString();
        String diagnosticId = JsonPath.read(diagnosticResponse, "$.id");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE support_case_diagnostics SET summary = ? WHERE id = ?::uuid",
                        "tampered",
                        diagnosticId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");

        mockMvc.perform(get("/api/v1/operations/search")
                        .with(asRole(PlatformRole.VIEWER, "viewer-subject", "viewer"))
                        .queryParam("query", "quasarflux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].type", containsInAnyOrder("INCIDENT", "DIAGNOSTIC")));

        mockMvc.perform(patch("/api/v1/support-cases/{supportCaseId}/status", supportCaseId)
                        .with(asRole(PlatformRole.RESPONDER, "responder-subject", "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "expectedVersion": 0,
                                  "resolutionNotes": "Connection pool was tuned and recovery verified."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.sla.health", is("MET")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(patch("/api/v1/support-cases/{supportCaseId}/status", supportCaseId)
                        .with(asRole(PlatformRole.RESPONDER, "responder-subject", "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED", "expectedVersion": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("stale_support_case_version")));

        Instant now = Instant.now();
        mockMvc.perform(get("/api/v1/operations/exports/cases")
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .queryParam("from", now.minusSeconds(86400).toString())
                        .queryParam("to", now.plusSeconds(86400).toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().string(containsString(caseReference)))
                .andExpect(content().string(not(containsString("customer.private@example.com"))))
                .andExpect(content().string(not(containsString("SECRET-CASE-DESCRIPTION"))))
                .andExpect(content().string(not(containsString("SECRET-DIAGNOSTIC"))));
    }

    private String createService() throws Exception {
        String response = mockMvc.perform(post("/api/v1/services")
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Milestone Three Gateway",
                                  "slug": "milestone-three-gateway",
                                  "description": "Customer gateway for support workflow tests"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createOrganisation() throws Exception {
        String response = mockMvc.perform(post("/api/v1/customer-organisations")
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Rhein Demo GmbH", "slug": "rhein-demo-gmbh"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void createSlaPolicy(String organisationId) throws Exception {
        mockMvc.perform(post(
                                "/api/v1/customer-organisations/{organisationId}/sla-policies",
                                organisationId)
                        .with(asRole(
                                PlatformRole.INCIDENT_MANAGER,
                                "manager-subject",
                                "incident-manager"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "P1",
                                  "responseTargetMinutes": 15,
                                  "resolutionTargetMinutes": 120,
                                  "warningThresholdPercent": 80
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private String createIncident(String serviceId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/incidents")
                        .with(asRole(PlatformRole.RESPONDER, "responder-subject", "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Quasarflux requests are slow",
                                  "description": "The quasarflux route shows elevated latency.",
                                  "severity": "SEV2",
                                  "affectedServiceId": "%s"
                                }
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createSupportCase(
            String organisationId, String serviceId, String incidentId) throws Exception {
        return mockMvc.perform(post("/api/v1/support-cases")
                        .with(asRole(PlatformRole.RESPONDER, "responder-subject", "responder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organisationId": "%s",
                                  "serviceId": "%s",
                                  "incidentId": "%s",
                                  "requesterName": "Private Customer",
                                  "requesterEmail": "customer.private@example.com",
                                  "subject": "Gateway latency",
                                  "description": "SECRET-CASE-DESCRIPTION",
                                  "priority": "P1",
                                  "runbookUrl": "https://runbooks.example.test/gateway"
                                }
                                """.formatted(organisationId, serviceId, incidentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.sla.health", is("ON_TRACK")))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn().getResponse().getContentAsString();
    }

    private static RequestPostProcessor asRole(
            PlatformRole role, String subject, String preferredUsername) {
        return jwt()
                .jwt(token -> token.subject(subject).claim("preferred_username", preferredUsername))
                .authorities(new SimpleGrantedAuthority(role.authority()));
    }
}
