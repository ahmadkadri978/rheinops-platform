package dev.rheinops.platform;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RheinOpsPlatformApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void managesAnIncidentLifecycleAndRejectsStaleUpdates() throws Exception {
        String serviceResponse = mockMvc.perform(post("/api/v1/services")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INVESTIGATING", "expectedVersion": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INVESTIGATING")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(patch("/api/v1/incidents/{incidentId}/status", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "IDENTIFIED", "expectedVersion": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("stale_incident_version")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));

        mockMvc.perform(get("/api/v1/incidents")
                        .queryParam("status", "INVESTIGATING")
                        .queryParam("severity", "SEV2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(incidentId)));
    }

    @Test
    void returnsProblemDetailsForInvalidRequests() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "slug": "INVALID SLUG"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("validation_failed")))
                .andExpect(jsonPath("$.violations", notNullValue()));
    }
}
