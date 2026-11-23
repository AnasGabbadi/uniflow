package com.isga.uniflow.inscription;

import com.isga.uniflow.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.isga.uniflow.TestData.unique;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flux de bout en bout (contrôleur -> service -> repository -> PostgreSQL), sans mock.
 * Les tests endpoint par endpoint relèvent des tests de contrôleur (IP-47) : ici on vérifie
 * que les couches fonctionnent ensemble sur une vraie base.
 *
 * Ces tests ne sont pas annulés (pas de @Transactional : les requêtes HTTP ont leurs propres
 * transactions). Les données ont donc des valeurs uniques et seules elles sont vérifiées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InscriptionFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void inscrireUnEtudiantDecrementeLesPlacesRestantesDeLaFormation() throws Exception {
        int idFormation = creerFormation(2);
        int idEtudiant = creerEtudiant();

        mvc.perform(get("/api/formations/{id}", idFormation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placesRestantes").value(2));

        mvc.perform(post("/api/inscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content(inscriptionJson(idEtudiant, idFormation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        mvc.perform(get("/api/formations/{id}", idFormation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placesRestantes").value(1));
    }

    @Test
    void inscrireDeuxFoisLeMemeEtudiantRenvoie409AuFormatProblemDetails() throws Exception {
        int idFormation = creerFormation(5);
        int idEtudiant = creerEtudiant();
        String inscription = inscriptionJson(idEtudiant, idFormation);

        mvc.perform(post("/api/inscriptions").contentType(MediaType.APPLICATION_JSON).content(inscription))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/inscriptions").contentType(MediaType.APPLICATION_JSON).content(inscription))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INSCRIPTION_EN_DOUBLE"))
                .andExpect(jsonPath("$.instance").value("/api/inscriptions"));

        // Le doublon refusé n'a pas consommé de place.
        mvc.perform(get("/api/formations/{id}", idFormation))
                .andExpect(jsonPath("$.placesRestantes").value(4));
    }

    // ---------- Outils ----------

    private int creerFormation(int capacite) throws Exception {
        String json = """
                {"intitule":"Flux %s","duree":12,"capaciteMax":%d,
                 "dateDebut":"2027-02-01","dateFin":"2027-02-10"}
                """.formatted(unique(), capacite);
        return idCree(post("/api/formations"), json);
    }

    private int creerEtudiant() throws Exception {
        String json = """
                {"nom":"Flux","prenom":"Test","email":"flux.%s@test.uniflow"}
                """.formatted(unique());
        return idCree(post("/api/etudiants"), json);
    }

    private int idCree(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                       String json) throws Exception {
        String reponse = mvc.perform(requete.contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reponse, "$.id");
    }

    // Noms de champs conformes au contrat d'API (docs/api-contract.md, §5)
    private static String inscriptionJson(int etudiantId, int formationId) {
        return """
                {"etudiantId":%d,"formationId":%d}
                """.formatted(etudiantId, formationId);
    }
}
