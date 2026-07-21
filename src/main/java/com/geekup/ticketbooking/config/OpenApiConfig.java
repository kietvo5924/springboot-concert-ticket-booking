package com.geekup.ticketbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeekUp Concert Ticket Booking API")
                        .version("1.0.0")
                        .description("Professional API Documentation for the Concert Ticket Booking platform, handling high-concurrency flash sales with Kafka, Redis, and PostgreSQL.")
                        .contact(new Contact()
                                .name("GeekUp Backend Candidate")
                                .email("candidate@geekup.vn")
                                .url("https://geekup.vn"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
