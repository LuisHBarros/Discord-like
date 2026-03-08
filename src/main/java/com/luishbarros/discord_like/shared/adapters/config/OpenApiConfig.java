// shared/adapters/config/OpenApiConfig.java
package com.luishbarros.discord_like.shared.adapters.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                )
                        )
                .info(new Info()
                                        .title("Discord-like API")
                                        .description("Real-time communication platform built with Spring Boot and Java 21")
                                        .version("1.0.0")
                                        .contact(new Contact()
                                                        .name("API Support")
                                                        .email("support@discord-like.com")
                                                        .url("https://github.com/your-org/discord-like/issues")
                                            )
                                        .license(new License()
                                                        .name("MIT License")
                                                        .url("https://opensource.org/licenses/MIT")
                                            )
                        );
    }
}
