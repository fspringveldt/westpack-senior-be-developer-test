package com.example.savings.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Savings Account API",
            version = "1.0.0",
            description =
                "RESTful API for managing customer savings accounts with multi-account limits, "
                    + "nickname validation, and distributed caching.",
            contact = @Contact(name = "API Support", url = "https://example.com/support"),
            license =
                @License(
                    name = "Apache 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0.html")),
    servers = {
      @Server(url = "http://localhost:8080", description = "Local development"),
      @Server(url = "https://api.example.com", description = "Production")
    })
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT authentication (for future use)")
public class OpenApiConfiguration {}
