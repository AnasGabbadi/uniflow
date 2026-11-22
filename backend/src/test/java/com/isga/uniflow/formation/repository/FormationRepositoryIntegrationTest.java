package com.isga.uniflow.formation.repository;

import com.isga.uniflow.TestcontainersConfiguration;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.formation.entity.Formation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.isga.uniflow.TestData.etudiant;
import static com.isga.uniflow.TestData.formation;
import static com.isga.uniflow.TestData.inscription;
import static com.isga.uniflow.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du repository Formation sur un vrai PostgreSQL : contraintes de la table formation (V1, V3)
 * et requêtes natives de comptage des inscriptions.
 *
 * Les comptages ne portent que sur les formations créées par le test : la base contient aussi le seed
 * V2 et d'autres données de test, aucun total global n'est vérifié.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class FormationRepositoryIntegrationTest {

    @Autowired
    private FormationRepository repository;

    @Autowired
    private TestEntityManager em;

    // ---------- Contraintes ----------

    @Test
    void intituleEnDoubleEstRefuseParLaBase() {
        String intitule = "Doublon " + unique();
        repository.saveAndFlush(formation(intitule));

        Formation doublon = formation(intitule);
        assertThatThrownBy(() -> repository.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("uq_formation_intitule");
    }

    @Test
    void existsByIntituleIgnoreCaseEtAndIdNot() {
        Formation f = repository.saveAndFlush(formation());

        assertThat(repository.existsByIntituleIgnoreCase(f.getIntitule().toUpperCase())).isTrue();
        assertThat(repository.existsByIntituleIgnoreCaseAndIdNot(f.getIntitule(), f.getId())).isFalse();
        assertThat(repository.existsByIntituleIgnoreCaseAndIdNot(f.getIntitule(), -1)).isTrue();
    }

    @Test
    void dateDeFinAvantLaDateDeDebutEstRefusee() {
        Formation f = formation();
        f.setDateDebut(LocalDate.of(2027, 3, 10));
        f.setDateFin(LocalDate.of(2027, 3, 9));

        assertThatThrownBy(() -> repository.saveAndFlush(f))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("ck_formation_dates");
    }

    @Test
    void dateDeFinEgaleALaDateDeDebutEstAcceptee() {
        Formation f = formation();
        f.setDateDebut(LocalDate.of(2027, 3, 10));
        f.setDateFin(LocalDate.of(2027, 3, 10));

        assertThat(repository.saveAndFlush(f).getId()).isNotNull();
    }

    @Test
    void datesAbsentesSontAccepteesCommePourLesAnciennesFormations() {
        Formation f = formation();
        f.setDateDebut(null);
        f.setDateFin(null);

        assertThat(repository.saveAndFlush(f).getId()).isNotNull();
    }

    @Test
    void dureeNulleEstRefusee() {
        Formation f = formation();
        f.setDuree(0);

        assertThatThrownBy(() -> repository.saveAndFlush(f))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("ck_formation_duree");
    }

    @Test
    void capaciteNegativeEstRefusee() {
        Formation f = formation();
        f.setCapaciteMax(-1);

        assertThatThrownBy(() -> repository.saveAndFlush(f))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("ck_formation_capacite");
    }

    @Test
    void statutInconnuEstRefuse() {
        Formation f = formation();
        f.setStatut("BROUILLON");

        assertThatThrownBy(() -> repository.saveAndFlush(f))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("ck_formation_statut");
    }

    // ---------- Requêtes natives de comptage ----------

    @Test
    void compteLesInscriptionsActivesEnAttenteEtValideesMaisPasAnnulees() {
        Formation f = em.persist(formation());
        inscrire(f, "EN_ATTENTE");
        inscrire(f, "VALIDEE");
        inscrire(f, "VALIDEE");
        inscrire(f, "ANNULEE");
        em.flush();

        assertThat(repository.compterInscriptionsActives(f.getId())).isEqualTo(3);
        assertThat(repository.compterToutesInscriptions(f.getId())).isEqualTo(4);
    }

    @Test
    void formationSansInscriptionACompteursAZero() {
        Formation f = em.persistAndFlush(formation());

        assertThat(repository.compterInscriptionsActives(f.getId())).isZero();
        assertThat(repository.compterToutesInscriptions(f.getId())).isZero();
    }

    @Test
    void compteLesInscriptionsActivesParFormationPourLesFormationsDuTest() {
        Formation deuxActives = em.persist(formation());
        inscrire(deuxActives, "EN_ATTENTE");
        inscrire(deuxActives, "VALIDEE");
        inscrire(deuxActives, "ANNULEE");
        Formation uneActive = em.persist(formation());
        inscrire(uneActive, "VALIDEE");
        Formation aucuneActive = em.persist(formation());
        inscrire(aucuneActive, "ANNULEE");
        em.flush();

        List<Object[]> lignes = repository.compterInscriptionsActivesParFormation();

        assertThat(nombrePour(lignes, deuxActives)).contains(2L);
        assertThat(nombrePour(lignes, uneActive)).contains(1L);
        // Une formation sans inscription active n'apparaît pas dans le résultat.
        assertThat(nombrePour(lignes, aucuneActive)).isEmpty();
    }

    // ---------- Outils ----------

    private void inscrire(Formation formation, String statut) {
        Etudiant etudiant = em.persist(etudiant());
        em.persist(inscription(etudiant, formation, statut));
    }

    private static Optional<Long> nombrePour(List<Object[]> lignes, Formation formation) {
        return lignes.stream()
                .filter(l -> ((Number) l[0]).intValue() == formation.getId())
                .map(l -> ((Number) l[1]).longValue())
                .findFirst();
    }
}
