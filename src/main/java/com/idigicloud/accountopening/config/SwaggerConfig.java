package com.idigicloud.accountopening.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "iDigi Bank — Account Opening Service API",
        version     = "1.0.0",
        description = "REST API for the 7-step Bank Account Opening workflow with CBS Mock integration",
        contact     = @Contact(name = "iDigi Cloud Technologies", email = "santosh.p@idigicloudtech.com")
    ),
    servers = {
        @Server(url = "/api", description = "Local development server")
    }
)
@SecurityScheme(
    name   = "bearerAuth",
    type   = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfig {
}
