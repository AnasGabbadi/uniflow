package com.isga.uniflow.etudiant.repository;

import com.isga.uniflow.TestcontainersConfiguration;
import com.isga.uniflow.etudiant.entity.Etudiant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.isga.uniflow.TestData.etudiant;
import static com.isga.uniflow.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du repository Etudiant sur un vrai PostgreSQL (Testcontainers).
 * Chaque test est annulé (rollback) à la fin : aucune donnée ne reste en base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EtudiantRepositoryIntegrationTest {

    @Autowired
    private EtudiantRepository repository;

    @Test
    void enregistreUnEtudiantEtGenereSonIdentifiant() {
        Etudiant enregistre = repository.saveAndFlush(etudiant());

        assertThat(enregistre.getId()).isNotNull();
        assertThat(repository.findById(enregistre.getId())).isPresent();
    }

    @Test
    void emailEnDoubleEstRefuseParLaBase() {
        String email = "doublon." + unique() + "@test.uniflow";
        repository.saveAndFlush(etudiant(email));

        Etudiant doublon = etudiant(email);
        assertThatThrownBy(() -> repository.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("uq_etudiant_email");
    }

    /**
     * La contrainte uq_etudiant_email de la base est SENSIBLE à la casse : deux
     * e-mails qui ne
     * diffèrent que par la casse sont acceptés par PostgreSQL. L'insensibilité à la
     * casse est
     * assurée par le service, qui appelle existsByEmailIgnoreCase avant
     * d'enregistrer.
     * Ce test documente ce comportement (le code applicatif n'est pas modifié).
     */
    @Test
    void laBaseEstSensibleALaCasseEtLeRepositoryDetecteLeDoublonSansCasse() {
        String suffixe = unique();
        repository.saveAndFlush(etudiant("casse." + suffixe + "@test.uniflow"));

        Etudiant majuscules = repository.saveAndFlush(etudiant("CASSE." + suffixe + "@TEST.UNIFLOW"));

        assertThat(majuscules.getId()).isNotNull();
        assertThat(repository.existsByEmailIgnoreCase("Casse." + suffixe + "@Test.Uniflow")).isTrue();
    }

    @Test
    void existsByEmailIgnoreCaseAndIdNotIgnoreLEtudiantLuiMeme() {
        Etudiant etudiant = repository.saveAndFlush(etudiant());

        assertThat(repository.existsByEmailIgnoreCaseAndIdNot(etudiant.getEmail(), etudiant.getId())).isFalse();
        assertThat(repository.existsByEmailIgnoreCaseAndIdNot(etudiant.getEmail(), -1)).isTrue();
    }

    @Test
    void rechercheParNomOuPrenomSansTenirCompteDeLaCasse() {
        String marqueur = "Zq" + unique();
        Etudiant parNom = etudiant();
        parNom.setNom("Nom" + marqueur);
        Etudiant parPrenom = etudiant();
        parPrenom.setPrenom(marqueur + "Prenom");
        repository.saveAndFlush(parNom);
        repository.saveAndFlush(parPrenom);

        List<Etudiant> resultat = repository.rechercherParNom(
                marqueur.toUpperCase(), Sort.by("id"));

        assertThat(resultat).extracting(Etudiant::getId)
                .containsExactly(parNom.getId(), parPrenom.getId());
    }

    @Test
    void rechercheParNomCompletDansLesDeuxOrdresSansTenirCompteDeLaCasse() {
        String suffixe = unique();
        Etudiant etudiant = etudiant();
        etudiant.setNom("Nz" + suffixe);
        etudiant.setPrenom("Pz" + suffixe);
        repository.saveAndFlush(etudiant);

        List<Etudiant> prenomPuisNom = repository.rechercherParNom(
                ("Pz" + suffixe + " Nz" + suffixe).toUpperCase(), Sort.by("id"));
        List<Etudiant> nomPuisPrenom = repository.rechercherParNom(
                ("nz" + suffixe + " pz" + suffixe).toLowerCase(), Sort.by("id"));

        assertThat(prenomPuisNom).extracting(Etudiant::getId).containsExactly(etudiant.getId());
        assertThat(nomPuisPrenom).extracting(Etudiant::getId).containsExactly(etudiant.getId());
    }

    @Test
    void rechercheSansResultatRenvoieUneListeVide() {
        repository.saveAndFlush(etudiant());

        List<Etudiant> resultat = repository.rechercherParNom("Introuvable-" + unique(), Sort.by("id"));

        assertThat(resultat).isEmpty();
    }
}
