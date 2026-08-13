package dev.rheinops.platform;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.rheinops.platform.shared.PlatformRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousApiRequestsReceiveAConsistentAuthenticationProblem() throws Exception {
        mockMvc.perform(get("/api/v1/incidents").header("X-Correlation-ID", "security-it-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code", is("authentication_required")))
                .andExpect(jsonPath("$.correlationId", is("security-it-401")));
    }

    @Test
    void viewerCannotRegisterAManagedService() throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .with(jwt()
                                .jwt(token -> token.subject("viewer-subject")
                                        .claim("preferred_username", "viewer"))
                                .authorities(new SimpleGrantedAuthority(
                                        PlatformRole.VIEWER.authority())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden Service",
                                  "slug": "forbidden-service",
                                  "description": "A viewer must not create this"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("insufficient_permissions")));
    }

    @Test
    void livenessInformationRemainsAvailableWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
