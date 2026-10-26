package com.ibam.produits;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI produitsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Produits")
                        .description("API REST de gestion des produits (CRUD, pagination, filtre par catégorie)")
                        .version("1.0.0"))
                .addServersItem(new Server().url("http://localhost:8080").description("Local"));
    }
}
