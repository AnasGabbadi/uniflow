package com.isga.uniflow.etudiant.service;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.RessourceIntrouvableException;
import com.isga.uniflow.etudiant.dto.EtudiantRequest;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EtudiantServiceTest {

    @Mock
    private EtudiantRepository repository;

    @InjectMocks
    private EtudiantService service;

    private EtudiantRequest requete() {
        return new EtudiantRequest("  El Amrani ", " Sara ", "  Sara.ElAmrani@Example.MA ",
                "+212 6 12 34 56 78", "12 rue des Écoles, Casablanca", LocalDate.of(2003, 4, 21));
    }

    // ---------- Création ----------

    @Test
    void creerEnregistreLEtudiantAvecLesDonneesNettoyees() {
        when(repository.existsByEmailIgnoreCase("sara.elamrani@example.ma")).thenReturn(false);
        when(repository.save(any(Etudiant.class))).thenAnswer(inv -> {
            Etudiant e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 7);   
            return e;
        });

        EtudiantResponse response = service.creer(requete());

        ArgumentCaptor<Etudiant> captor = ArgumentCaptor.forClass(Etudiant.class);
        verify(repository).save(captor.capture());
        Etudiant saved = captor.getValue();

        assertEquals("El Amrani", saved.getNom());                       
        assertEquals("Sara", saved.getPrenom());                         
        assertEquals("sara.elamrani@example.ma", saved.getEmail());      
        assertEquals("+212 6 12 34 56 78", saved.getTelephone());
        assertEquals("12 rue des Écoles, Casablanca", saved.getAdresse());
        assertEquals(LocalDate.of(2003, 4, 21), saved.getDateNaissance());

        assertEquals(7, response.id());
        assertEquals("sara.elamrani@example.ma", response.email());
        assertEquals("El Amrani", response.nom());
    }

    @Test
    void creerAccepteLesChampsFacultatifsAbsents() {
        EtudiantRequest minimale = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma", null, null, null);
        when(repository.existsByEmailIgnoreCase("sara@example.ma")).thenReturn(false);
        when(repository.save(any(Etudiant.class))).thenAnswer(inv -> inv.getArgument(0));

        EtudiantResponse response = service.creer(minimale);

        assertEquals("sara@example.ma", response.email());
        assertNull(response.telephone());
        assertNull(response.adresse());
        assertNull(response.dateNaissance());
    }

    @Test
    void creerRefuseUnEmailDejaUtilise() {
        when(repository.existsByEmailIgnoreCase("sara.elamrani@example.ma")).thenReturn(true);

        ConflitMetierException exception = assertThrows(ConflitMetierException.class,
                () -> service.creer(requete()));

        assertEquals(CodeErreur.EMAIL_DEJA_UTILISE, exception.getCode());
        verify(repository, never()).save(any());
    }

    // ---------- Consultation : détail ----------

    @Test
    void trouverParIdRenvoieLEtudiant() {
        when(repository.findById(5)).thenReturn(Optional.of(unEtudiant(5, "El Amrani", "Sara", "sara@example.ma")));

        EtudiantResponse response = service.trouverParId(5);

        assertEquals(5, response.id());
        assertEquals("El Amrani", response.nom());
        assertEquals("Sara", response.prenom());
        assertEquals("sara@example.ma", response.email());
    }

    @Test
    void trouverParIdLeve404SiIdentifiantInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        RessourceIntrouvableException exception = assertThrows(RessourceIntrouvableException.class,
                () -> service.trouverParId(99));

        assertEquals("Aucun étudiant avec l'identifiant 99.", exception.getMessage());
    }

    // ---------- Consultation : liste et recherche ----------

    @Test
    void listerSansRechercheRenvoieTousLesEtudiantsTriesParNomPuisPrenom() {
        Sort tri = Sort.by("nom", "prenom");
        when(repository.findAll(tri)).thenReturn(List.of(
                unEtudiant(1, "Alaoui", "Rania", "rania@example.ma"),
                unEtudiant(2, "Benali", "Yassine", "yassine@example.ma")));

        List<EtudiantResponse> reponses = service.lister(null);

        assertEquals(2, reponses.size());
        assertEquals("Alaoui", reponses.get(0).nom());
        assertEquals("Benali", reponses.get(1).nom());
    }

    @Test
    void listerAvecRechercheVideRenvoieTousLesEtudiants() {
        Sort tri = Sort.by("nom", "prenom");
        when(repository.findAll(tri)).thenReturn(List.of(unEtudiant(1, "Alaoui", "Rania", "rania@example.ma")));

        List<EtudiantResponse> reponses = service.lister("   ");

        assertEquals(1, reponses.size());
        verify(repository, never())
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void listerAvecRechercheCherchePartiellementSurNomOuPrenom() {
        Sort tri = Sort.by("nom", "prenom");
        when(repository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase("ali", "ali", tri))
                .thenReturn(List.of(unEtudiant(3, "Alibi", "Omar", "omar@example.ma")));

        List<EtudiantResponse> reponses = service.lister("  ali ");   

        assertEquals(1, reponses.size());
        assertEquals("Alibi", reponses.get(0).nom());
        verify(repository, never()).findAll(any(Sort.class));
    }

    @Test
    void listerRenvoieUneListeVideSiAucunResultat() {
        Sort tri = Sort.by("nom", "prenom");
        when(repository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase("zzz", "zzz", tri))
                .thenReturn(List.of());

        assertEquals(0, service.lister("zzz").size());
    }

    // ---------- Modification  ----------

    @Test
    void modifierMetAJourLEtudiantAvecLesDonneesNettoyees() {
        Etudiant existant = unEtudiant(7, "Ancien", "Nom", "ancien@example.ma");
        when(repository.findById(7)).thenReturn(Optional.of(existant));
        when(repository.existsByEmailIgnoreCaseAndIdNot("sara.elamrani@example.ma", 7)).thenReturn(false);
        when(repository.save(any(Etudiant.class))).thenAnswer(inv -> inv.getArgument(0));

        EtudiantResponse response = service.modifier(7, requete());

        ArgumentCaptor<Etudiant> captor = ArgumentCaptor.forClass(Etudiant.class);
        verify(repository).save(captor.capture());
        Etudiant saved = captor.getValue();

        assertEquals(7, saved.getId());                                  
        assertEquals("El Amrani", saved.getNom());                       
        assertEquals("Sara", saved.getPrenom());                         
        assertEquals("sara.elamrani@example.ma", saved.getEmail());      
        assertEquals("+212 6 12 34 56 78", saved.getTelephone());
        assertEquals(LocalDate.of(2003, 4, 21), saved.getDateNaissance());

        assertEquals(7, response.id());
        assertEquals("sara.elamrani@example.ma", response.email());
    }

    @Test
    void modifierLeve404SiIdentifiantInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> service.modifier(99, requete()));

        verify(repository, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void modifierRefuseUnEmailUtiliseParUnAutreEtudiant() {
        when(repository.findById(7)).thenReturn(Optional.of(unEtudiant(7, "Ancien", "Nom", "ancien@example.ma")));
        when(repository.existsByEmailIgnoreCaseAndIdNot("sara.elamrani@example.ma", 7)).thenReturn(true);

        ConflitMetierException exception = assertThrows(ConflitMetierException.class,
                () -> service.modifier(7, requete()));

        assertEquals(CodeErreur.EMAIL_DEJA_UTILISE, exception.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void creerIndiqueLEmailEnConflitDansLeMessage() {
        when(repository.existsByEmailIgnoreCase("sara.elamrani@example.ma")).thenReturn(true);

        ConflitMetierException exception = assertThrows(ConflitMetierException.class, () -> service.creer(requete()));

        assertEquals("L'email 'sara.elamrani@example.ma' est déjà utilisé par un autre étudiant.", exception.getMessage());
    }
    


    private Etudiant unEtudiant(int id, String nom, String prenom, String email) {
        Etudiant e = new Etudiant();
        ReflectionTestUtils.setField(e, "id", id);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setEmail(email);
        return e;
    }
}
