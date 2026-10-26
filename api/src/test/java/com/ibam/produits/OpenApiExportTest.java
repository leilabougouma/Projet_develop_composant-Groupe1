package com.ibam.produits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Exporte le contrat généré par springdoc dans api/openapi.yaml.
 * Lancer avec : mvn test -Dtest=OpenApiExportTest -Dopenapi.export=true
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "openapi.export", matches = "true")
class OpenApiExportTest {

    @Autowired
    MockMvc mvc;

    @Test
    void exporterLeContrat() throws Exception {
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        Files.writeString(Path.of("openapi.yaml"), yaml, StandardCharsets.UTF_8);
    }
}
