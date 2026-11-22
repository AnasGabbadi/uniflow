package com.isga.uniflow;

import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.inscription.entity.Inscription;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fabrique de données de test aux valeurs UNIQUES.
 *
 * La base de test contient aussi le seed V2 et les données des tests @SpringBootTest (non annulées) :
 * les tests ne doivent jamais dépendre de ces données, ni asserter sur des totaux globaux.
 */
public final class TestData {

    private TestData() {
    }

    /** Suffixe court et unique (8 caractères hexadécimaux). */
    public static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static Etudiant etudiant() {
        return etudiant("etu." + unique() + "@test.uniflow");
    }

    public static Etudiant etudiant(String email) {
        Etudiant e = new Etudiant();
        e.setNom("Nom-" + unique());
        e.setPrenom("Prenom");
        e.setEmail(email);
        return e;
    }

    public static Formation formation() {
        return formation("Formation IT " + unique());
    }

    public static Formation formation(String intitule) {
        Formation f = new Formation();
        f.setIntitule(intitule);
        f.setDuree(20);
        f.setCapaciteMax(10);
        f.setDateDebut(LocalDate.of(2027, 1, 10));
        f.setDateFin(LocalDate.of(2027, 1, 20));
        return f;
    }

    public static Inscription inscription(Etudiant etudiant, Formation formation, String statut) {
        Inscription i = new Inscription();
        i.setEtudiant(etudiant);
        i.setFormation(formation);
        i.setStatut(statut);
        return i;
    }
}
