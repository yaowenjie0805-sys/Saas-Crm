package com.yao.crm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI crmOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CRM API")
                .description("CRM System REST API Documentation - Multi-tenant Customer Relationship Management")
                .version("1.0.0")
                .contact(new Contact()
                    .name("CRM Team")
                    .email("crm@yao.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
