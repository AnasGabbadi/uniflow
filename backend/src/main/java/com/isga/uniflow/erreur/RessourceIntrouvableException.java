package com.isga.uniflow.erreur;

/** Levée quand un identifiant ne correspond à aucune ressource (HTTP 404). */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String ressource, Object id) {
        super("Aucun %s avec l'identifiant %s.".formatted(ressource, id));
    }
}
