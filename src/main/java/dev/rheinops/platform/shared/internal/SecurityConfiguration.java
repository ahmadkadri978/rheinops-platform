package dev.rheinops.platform.shared.internal;

import dev.rheinops.platform.shared.PlatformRole;
import java.time.Clock;
import java.util.Collection;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtSecurityProperties.class, ApiRateLimitProperties.class})
public class SecurityConfiguration {

    private static final String[] READ_ROLES = roleNames(
            PlatformRole.VIEWER,
            PlatformRole.RESPONDER,
            PlatformRole.INCIDENT_MANAGER,
            PlatformRole.ADMINISTRATOR);
    private static final String[] RESPONSE_ROLES = roleNames(
            PlatformRole.RESPONDER,
            PlatformRole.INCIDENT_MANAGER,
            PlatformRole.ADMINISTRATOR);
    private static final String[] MANAGEMENT_ROLES = roleNames(
            PlatformRole.INCIDENT_MANAGER, PlatformRole.ADMINISTRATOR);

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ApiRateLimitProperties rateLimitProperties,
            Clock clock)
            throws Exception {
        ApiProblemWriter problemWriter = new ApiProblemWriter(clock);
        ApiAuthenticationEntryPoint authenticationEntryPoint =
                new ApiAuthenticationEntryPoint(problemWriter);
        ApiAccessDeniedHandler accessDeniedHandler = new ApiAccessDeniedHandler(problemWriter);

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/actuator/prometheus")
                        .hasRole(PlatformRole.ADMINISTRATOR.name())
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**")
                        .hasAnyRole(READ_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/operations/exports/**")
                        .hasAnyRole(MANAGEMENT_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**")
                        .hasAnyRole(READ_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/services")
                        .hasAnyRole(MANAGEMENT_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/customer-organisations/**")
                        .hasAnyRole(MANAGEMENT_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/incidents")
                        .hasAnyRole(RESPONSE_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/support-cases/**")
                        .hasAnyRole(RESPONSE_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/**")
                        .hasAnyRole(RESPONSE_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/support-cases/**")
                        .hasAnyRole(RESPONSE_ROLES)
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .headers(Customizer.withDefaults())
                .addFilterBefore(
                        new ApiRateLimitFilter(rateLimitProperties, problemWriter),
                        BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                        properties.jwkSetUri().toString())
                .build();
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.issuerUri().toString());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, new AudienceValidator(properties.audience())));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter =
                new KeycloakRealmRoleConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private static String[] roleNames(PlatformRole... roles) {
        return java.util.Arrays.stream(roles).map(Enum::name).toArray(String[]::new);
    }
}
