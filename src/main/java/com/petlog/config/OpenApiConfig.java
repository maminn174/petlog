package com.petlog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Petlog API",
        version = "0.1.0",
        description = "REST API for tracking pet activities, managing pet profiles, and sharing pet care responsibilities"
    )
)
public class OpenApiConfig {
}
