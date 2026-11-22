package com.isga.uniflow.erreur;

/**
 * Levée quand une règle métier empêche l'opération (HTTP 409).
 * Exemple : {@code throw new ConflitMetierException(CodeErreur.FORMATION_COMPLETE, "La formation est complète.");}
 */
public class ConflitMetierException extends RuntimeException {

    private final CodeErreur code;

    public ConflitMetierException(CodeErreur code, String message) {
        super(message);
        this.code = code;
    }

    public CodeErreur getCode() {
        return code;
    }
}
