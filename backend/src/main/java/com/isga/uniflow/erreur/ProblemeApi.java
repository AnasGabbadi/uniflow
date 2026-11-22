package com.isga.uniflow.erreur;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Réponse d'erreur au format Problem Details (RFC 9457), complétée par {@code code} et {@code erreurs}.
 * Les deux champs sont de vraies propriétés : ils apparaissent toujours au premier niveau du JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemeApi extends ProblemDetail {

    private final CodeErreur code;
    private List<ErreurChamp> erreurs;

    public ProblemeApi(ProblemDetail source, CodeErreur code) {
        super(source);
        this.code = code;
    }

    public static ProblemeApi creer(HttpStatusCode statut, String titre, String detail, CodeErreur code) {
        ProblemDetail base = ProblemDetail.forStatusAndDetail(statut, detail);
        base.setTitle(titre);
        return new ProblemeApi(base, code);
    }

    public CodeErreur getCode() {
        return code;
    }

    public List<ErreurChamp> getErreurs() {
        return erreurs;
    }

    public void setErreurs(List<ErreurChamp> erreurs) {
        this.erreurs = erreurs;
    }
}
