package com.ibam.produits.controller;

import com.ibam.produits.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test d'intégration de bout en bout : contrôleur -> service -> JPA -> base H2 (mode PostgreSQL). */
@SpringBootTest
@AutoConfigureMockMvc
class ProduitControllerIntegrationTest {

    private static final String BASE = "/api/v1/produits";
    private static final String VALIDE = """
            {"nom":"Clavier","description":"AZERTY","prix":59.90,"quantite":25,"categorie":"Informatique"}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    ProduitRepository repository;

    @BeforeEach
    void viderLaTable() {
        repository.deleteAll();
    }

    private int creer(String json) throws Exception {
        MvcResult r = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        String body = r.getResponse().getContentAsString();
        return Integer.parseInt(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void creerRenvoie201AvecLocationEtDateCreation() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALIDE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nom").value("Clavier"))
                .andExpect(jsonPath("$.prix").value(59.90))
                .andExpect(jsonPath("$.dateCreation").isNotEmpty());
    }

    @Test
    void consulterRenvoie200() throws Exception {
        int id = creer(VALIDE);

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.categorie").value("Informatique"));
    }

    @Test
    void consulterAbsentRenvoie404() throws Exception {
        mvc.perform(get(BASE + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("9999")))
                .andExpect(jsonPath("$.path").value(BASE + "/9999"));
    }

    @Test
    void identifiantNonNumeriqueRenvoie400() throws Exception {
        mvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("id")));
    }

    @Test
    void modifierRenvoie200EtConserveLaDateDeCreation() throws Exception {
        int id = creer(VALIDE);
        String avant = mvc.perform(get(BASE + "/" + id)).andReturn().getResponse().getContentAsString();

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Clavier RGB","prix":79.00,"quantite":3,"categorie":"Informatique"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Clavier RGB"))
                .andExpect(jsonPath("$.quantite").value(3));

        String apres = mvc.perform(get(BASE + "/" + id)).andReturn().getResponse().getContentAsString();
        assertThat(apres.replaceAll(".*\"dateCreation\":\"([^\"]+)\".*", "$1"))
                .isEqualTo(avant.replaceAll(".*\"dateCreation\":\"([^\"]+)\".*", "$1"));
    }

    @Test
    void modifierAbsentRenvoie404() throws Exception {
        mvc.perform(put(BASE + "/9999").contentType(MediaType.APPLICATION_JSON).content(VALIDE))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimerRenvoie200AvecConfirmationPuisLeProduitDisparait() throws Exception {
        int id = creer(VALIDE);

        mvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Produit " + id + " supprimé"))
                .andExpect(jsonPath("$.produit.id").value(id))
                .andExpect(jsonPath("$.produit.nom").value("Clavier"));
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void supprimerAbsentRenvoie404() throws Exception {
        mvc.perform(delete(BASE + "/9999")).andExpect(status().isNotFound());
    }

    @Test
    void listerPagineEtFiltreParCategorie() throws Exception {
        for (int i = 1; i <= 3; i++) {
            creer("{\"nom\":\"Info " + i + "\",\"prix\":10,\"quantite\":1,\"categorie\":\"Informatique\"}");
        }
        creer("{\"nom\":\"Roman\",\"prix\":8,\"quantite\":1,\"categorie\":\"Livres\"}");

        mvc.perform(get(BASE).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get(BASE).param("categorie", "informatique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get(BASE).param("categorie", "Inconnue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void listerAvecPaginationInvalideRenvoie400() throws Exception {
        mvc.perform(get(BASE).param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("size", "1000")).andExpect(status().isBadRequest());
    }

    @Test
    void creerAvecDonneesInvalidesRenvoie400AvecDetails() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"  ","prix":-5,"quantite":-1,"categorie":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.length()").value(4));
    }

    @Test
    void creerSansPrixRenvoie400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"X\",\"quantite\":1,\"categorie\":\"Y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].champ").value("prix"));
    }

    @Test
    void jsonMalFormeRenvoie400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void modifierAvecDonneesInvalidesRenvoie400() throws Exception {
        int id = creer(VALIDE);
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"prix\":1,\"quantite\":1,\"categorie\":\"Y\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void documentationOpenApiEstExposee() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/produits']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/produits/{id}']").exists());
    }

    @Test
    void routeInconnueRenvoie404EnJson() throws Exception {
        mvc.perform(get("/api/inconnu"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void racineRedirigeVersSwagger() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
    }

    @Test
    void actuatorHealthEstExpose() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
