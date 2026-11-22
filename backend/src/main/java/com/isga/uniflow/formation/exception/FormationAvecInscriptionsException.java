package com.isga.uniflow.formation.exception;

/**
 * Levée quand on essaie de supprimer une formation qui a encore des inscriptions (quel que soit leur
 * statut, y compris ANNULEE : elles font partie de l'historique et la base les référence).
 * Renvoyée en HTTP 409 par {@link FormationExceptionHandler}.
 */
public class FormationAvecInscriptionsException extends RuntimeException {

    public FormationAvecInscriptionsException(Integer id, long nombreInscriptions) {
        super("La formation " + id + " ne peut pas être supprimée : elle compte "
                + nombreInscriptions + " inscription(s)");
    }
}
