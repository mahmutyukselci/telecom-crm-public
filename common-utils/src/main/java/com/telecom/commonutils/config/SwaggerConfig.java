package com.telecom.commonutils.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "KeycloakAuth"),
        servers = { @Server(url = "/", description = "API Gateway") }
)
@SecurityScheme(
        name = "KeycloakAuth",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "http://localhost:8081/realms/telecom-realm/protocol/openid-connect/auth",
                        tokenUrl = "http://localhost:8081/realms/telecom-realm/protocol/openid-connect/token"
                )
        )
)
public class SwaggerConfig {


}