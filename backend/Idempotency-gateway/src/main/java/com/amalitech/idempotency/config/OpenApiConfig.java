package com.amalitech.idempotency.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI idempotencyGatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Idempotency-Gateway")
                .version("1.0.0")
                .description("""
                        Idempotency layer for payment processing.

                        Every request must carry an `Idempotency-Key` header.
                        Retries with the same key replay the original response
                        (signalled by `X-Cache-Hit: true`). The same key with a
                        different body returns 409 Conflict. Idempotency entries
                        expire 24 hours after creation.
                        """)
                .contact(new Contact()
                        .name("Patrick Nshimiyimana - AmaliTech Project based challenge")
                        .url("https://github.com/Patricknshimiyimana/AmaliTech-DEG-Project-based-challenges"))
                .license(new License().name("MIT")));
    }
}
