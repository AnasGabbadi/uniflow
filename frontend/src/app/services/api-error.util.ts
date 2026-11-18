import { HttpErrorResponse } from '@angular/common/http';

export interface ErreurAffichable {
  message: string;
  code?: string;
  /** Messages d'erreur par champ, indexés par le nom du champ côté API. */
  champs: Record<string, string>;
}

const MESSAGE_PAR_DEFAUT = 'An unexpected error occurred. Please try again.';

/**
 * Transforme une erreur HTTP en message lisible.
 * Gère le format Problem Details (IP-22 : detail, code, erreurs[]) et l'ancien format
 * (message, erreurs {champ: message}) encore utilisé par certains modules.
 */
export function lireErreurApi(erreur: unknown): ErreurAffichable {
  if (!(erreur instanceof HttpErrorResponse)) {
    return { message: MESSAGE_PAR_DEFAUT, champs: {} };
  }
  if (erreur.status === 0) {
    return { message: 'Unable to reach the server. Check that the API is running.', champs: {} };
  }

  const resultat: ErreurAffichable = { message: MESSAGE_PAR_DEFAUT, champs: {} };
  const corps: unknown = erreur.error;
  if (!corps || typeof corps !== 'object') {
    return resultat;
  }

  const donnees = corps as Record<string, unknown>;
  if (typeof donnees['detail'] === 'string') {
    resultat.message = donnees['detail'];
  } else if (typeof donnees['message'] === 'string') {
    resultat.message = donnees['message'];
  }
  if (typeof donnees['code'] === 'string') {
    resultat.code = donnees['code'];
  }

  const erreurs = donnees['erreurs'];
  if (Array.isArray(erreurs)) {
    for (const element of erreurs as { champ?: unknown; message?: unknown }[]) {
      if (typeof element.champ === 'string' && typeof element.message === 'string') {
        resultat.champs[element.champ] = element.message;
      }
    }
  } else if (erreurs && typeof erreurs === 'object') {
    for (const [champ, message] of Object.entries(erreurs)) {
      if (typeof message === 'string') {
        resultat.champs[champ] = message;
      }
    }
  }
  return resultat;
}
