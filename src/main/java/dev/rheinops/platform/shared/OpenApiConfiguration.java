package dev.rheinops.platform.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "OIDC access token issued for the rheinops-api audience")
public class OpenApiConfiguration {

    @Bean
    OpenAPI rheinOpsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("RheinOps Platform API")
                .version("v1")
                .description("Service operations and incident lifecycle API")
                .contact(new Contact()
                        .name("Ahmad Kadri")
                        .url("https://github.com/ahmadkadri978"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT")));
    }
}
