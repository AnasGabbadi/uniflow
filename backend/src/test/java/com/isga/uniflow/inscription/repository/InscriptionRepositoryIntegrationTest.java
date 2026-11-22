package com.isga.uniflow.inscription.repository;

import com.isga.uniflow.TestcontainersConfiguration;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.repository.FormationRepository;
import com.isga.uniflow.inscription.entity.Inscription;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static com.isga.uniflow.TestData.etudiant;
import static com.isga.uniflow.TestData.formation;
import static com.isga.uniflow.TestData.inscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du repository Inscription sur un vrai PostgreSQL : relations avec Etudiant et Formation,
 * contraintes de la table inscription (unicité, clés étrangères, ON DELETE CASCADE, CHECK).
 *
 * Les suppressions en cascade sont testées à deux niveaux :
 * - en SQL natif (JdbcTemplate), qui contourne JPA : prouve le ON DELETE CASCADE de la base ;
 * - via le repository JPA : prouve le cascade = REMOVE des entités.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class InscriptionRepositoryIntegrationTest {

    @Autowired
    private InscriptionRepository repository;

    @Autowired
    private FormationRepository formationRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    // ---------- Enregistrement et relations ----------

    @Test
    void enregistreUneInscriptionAvecSesValeursParDefaut() {
        Etudiant etudiant = em.persist(etudiant());
        Formation formation = em.persist(formation());
        Inscription inscription = new Inscription();
        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);

        Inscription enregistree = repository.saveAndFlush(inscription);
        em.clear();

        Inscription relue = repository.findByIdAvecDetails(enregistree.getId()).orElseThrow();
        assertThat(relue.getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(relue.getDateInscription()).isEqualTo(LocalDate.now());
        assertThat(relue.getEtudiant().getId()).isEqualTo(etudiant.getId());
        assertThat(relue.getFormation().getIntitule()).isEqualTo(formation.getIntitule());
    }

    @Test
    void findAllAvecDetailsChargeEtudiantEtFormationSansRequeteParesseuse() {
        Etudiant etudiant = em.persist(etudiant());
        Formation formation = em.persist(formation());
        Inscription inscription = em.persist(inscription(etudiant, formation, "VALIDEE"));
        em.flush();
        em.clear();

        List<Inscription> toutes = repository.findAllAvecDetails();
        // On détache tout : un accès paresseux non chargé lèverait LazyInitializationException.
        em.clear();

        Inscription trouvee = toutes.stream()
                .filter(i -> i.getId().equals(inscription.getId()))
                .findFirst().orElseThrow();
        assertThat(trouvee.getEtudiant().getEmail()).isEqualTo(etudiant.getEmail());
        assertThat(trouvee.getFormation().getIntitule()).isEqualTo(formation.getIntitule());
    }

    @Test
    void existsByEtudiantEtFormationEtComptageParStatut() {
        Etudiant etudiant = em.persist(etudiant());
        Etudiant autre = em.persist(etudiant());
        Formation formation = em.persist(formation());
        em.persist(inscription(etudiant, formation, "VALIDEE"));
        em.persist(inscription(autre, formation, "ANNULEE"));
        em.flush();

        assertThat(repository.existsByEtudiant_IdAndFormation_Id(etudiant.getId(), formation.getId())).isTrue();
        assertThat(repository.existsByEtudiant_IdAndFormation_Id(etudiant.getId(), -1)).isFalse();
        assertThat(repository.countByFormation_IdAndStatutIn(formation.getId(), List.of("EN_ATTENTE", "VALIDEE")))
                .isEqualTo(1);
    }

    // ---------- Contraintes ----------

    @Test
    void memeEtudiantNePeutPasSInscrireDeuxFoisALaMemeFormation() {
        Etudiant etudiant = em.persist(etudiant());
        Formation formation = em.persist(formation());
        repository.saveAndFlush(inscription(etudiant, formation, "ANNULEE"));

        // Le doublon est refusé quel que soit le statut de la première inscription.
        Inscription doublon = inscription(etudiant, formation, "EN_ATTENTE");
        assertThatThrownBy(() -> repository.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().hasMessageContaining("uq_inscription_etudiant_formation");
    }

    @Test
    void inscriptionVersUnEtudiantInexistantEstRefusee() {
        Integer idFormation = em.persistAndFlush(formation()).getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO inscription (id_etudiant, id_formation) VALUES (?, ?)", -1, idFormation))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_inscription_etudiant");
    }

    @Test
    void inscriptionVersUneFormationInexistanteEstRefusee() {
        Integer idEtudiant = em.persistAndFlush(etudiant()).getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO inscription (id_etudiant, id_formation) VALUES (?, ?)", idEtudiant, -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_inscription_formation");
    }

    /** Côté JPA, la validation de l'entité (@Pattern sur statut) refuse le statut avant d'atteindre la base. */
    @Test
    void statutDInscriptionInconnuEstRefuseParLaValidationJpa() {
        Etudiant etudiant = em.persist(etudiant());
        Formation formation = em.persist(formation());

        Inscription invalide = inscription(etudiant, formation, "REFUSEE");
        assertThatThrownBy(() -> repository.saveAndFlush(invalide))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("statut");
    }

    /** En SQL natif (sans passer par JPA), c'est la contrainte CHECK de la base qui refuse le statut. */
    @Test
    void statutDInscriptionInconnuEstRefuseParLaBase() {
        Integer idEtudiant = em.persist(etudiant()).getId();
        Integer idFormation = em.persistAndFlush(formation()).getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO inscription (id_etudiant, id_formation, statut) VALUES (?, ?, 'REFUSEE')",
                idEtudiant, idFormation))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_inscription_statut");
    }

    // ---------- ON DELETE CASCADE (base de données, SQL natif) ----------

    @Test
    void supprimerUneFormationEnSqlSupprimeSesInscriptionsEnCascade() {
        Formation formation = em.persist(formation());
        Etudiant etudiant = em.persist(etudiant());
        Inscription inscription = em.persist(inscription(etudiant, formation, "VALIDEE"));
        em.flush();
        em.clear();

        jdbc.update("DELETE FROM formation WHERE id_formation = ?", formation.getId());

        assertThat(lignesInscription(inscription.getId())).isZero();
        // L'étudiant n'est pas concerné par la cascade.
        assertThat(etudiantRepository.findById(etudiant.getId())).isPresent();
    }

    @Test
    void supprimerUnEtudiantEnSqlSupprimeSesInscriptionsEnCascade() {
        Formation formation = em.persist(formation());
        Etudiant etudiant = em.persist(etudiant());
        Inscription inscription = em.persist(inscription(etudiant, formation, "EN_ATTENTE"));
        em.flush();
        em.clear();

        jdbc.update("DELETE FROM etudiant WHERE id_etudiant = ?", etudiant.getId());

        assertThat(lignesInscription(inscription.getId())).isZero();
        assertThat(formationRepository.findById(formation.getId())).isPresent();
    }

    // ---------- cascade = REMOVE (JPA) ----------

    @Test
    void supprimerUneFormationViaJpaSupprimeSesInscriptions() {
        Formation formation = em.persist(formation());
        Inscription i1 = em.persist(inscription(em.persist(etudiant()), formation, "VALIDEE"));
        Inscription i2 = em.persist(inscription(em.persist(etudiant()), formation, "ANNULEE"));
        em.flush();
        em.clear();

        formationRepository.delete(formationRepository.findById(formation.getId()).orElseThrow());
        formationRepository.flush();

        assertThat(lignesInscription(i1.getId())).isZero();
        assertThat(lignesInscription(i2.getId())).isZero();
    }

    @Test
    void supprimerUnEtudiantViaJpaSupprimeSesInscriptions() {
        Etudiant etudiant = em.persist(etudiant());
        Inscription inscription = em.persist(inscription(etudiant, em.persist(formation()), "VALIDEE"));
        em.flush();
        em.clear();

        etudiantRepository.delete(etudiantRepository.findById(etudiant.getId()).orElseThrow());
        etudiantRepository.flush();

        assertThat(lignesInscription(inscription.getId())).isZero();
    }

    private int lignesInscription(Integer id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM inscription WHERE id_inscription = ?", Integer.class, id);
        return n == null ? 0 : n;
    }
}
