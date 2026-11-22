package com.isga.uniflow.erreur;

/** Erreur de validation sur un champ de la requête. */
public record ErreurChamp(String champ, String message) {
}
