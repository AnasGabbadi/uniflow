import { HttpErrorResponse } from '@angular/common/http';

import { CodeErreur } from '../models/api-error.model';
import { lireErreurApi } from './api-error.util';

/** Message anglais affiché pour chaque code d'erreur de l'API (docs/api-contract.md §6). */
export const MESSAGES_PAR_CODE: Record<CodeErreur, string> = {
  VALIDATION: 'Some fields are invalid. Please check the form.',
  REQUETE_INVALIDE: 'The request is invalid. Please check the entered values.',
  RESSOURCE_INTROUVABLE: 'The requested item was not found. It may have been deleted.',
  EMAIL_DEJA_UTILISE: 'This email address is already used by another student.',
  INTITULE_DEJA_UTILISE: 'A training with this title already exists.',
  CAPACITE_INSUFFISANTE: 'The capacity cannot be lower than the number of active enrollments.',
  INSCRIPTION_EN_DOUBLE: 'This student is already enrolled in this training.',
  FORMATION_COMPLETE: 'This training has reached its maximum capacity.',
  FORMATION_NON_OUVERTE: 'This training is not open for enrollment.',
  TRANSITION_INTERDITE: 'This status change is not allowed.',
  ERREUR_INTERNE: 'An internal server error occurred. Please try again later.',
};

export const MESSAGE_SERVEUR_INJOIGNABLE =
  'Unable to reach the server. Check your connection or try again in a moment.';

export const MESSAGE_INATTENDU = 'An unexpected error occurred. Please try again.';

/** Message par statut HTTP, utilisé quand l'API ne fournit ni code connu ni texte. */
const MESSAGES_PAR_STATUT: Record<number, string> = {
  400: 'The request is invalid. Please check the entered values.',
  404: 'The requested item was not found. It may have been deleted.',
  409: 'This action conflicts with existing data.',
  500: 'An internal server error occurred. Please try again later.',
};

/** Statuts traités comme « serveur injoignable » : réseau coupé (0) ou API arrêtée derrière Nginx. */
const STATUTS_INJOIGNABLE = new Set([0, 502, 503, 504]);

/** Surcharges propres à une page, par code d'erreur ou par statut HTTP. */
export type SurchargesMessages = Partial<Record<CodeErreur | number, string>>;

/** Vrai si l'erreur signifie que le serveur est injoignable (réseau ou passerelle). */
export function estServeurInjoignable(erreur: unknown): boolean {
  return erreur instanceof HttpErrorResponse && STATUTS_INJOIGNABLE.has(erreur.status);
}

/**
 * Transforme n'importe quelle erreur en message anglais lisible. Ordre de priorité :
 * 1. surcharge de la page (par code, puis par statut) ;
 * 2. message associé au code de l'API ;
 * 3. texte de l'API (`detail` Problem Details, ou `message` de l'ancien format) ;
 * 4. message selon le statut HTTP (0, 502, 503, 504 = serveur injoignable ; 400, 404, 409, 5xx) ;
 * 5. message générique.
 *
 * Les champs en erreur (`champs`) restent fournis par `lireErreurApi`.
 */
export function messageErreurApi(erreur: unknown, surcharges: SurchargesMessages = {}): string {
  if (!(erreur instanceof HttpErrorResponse)) {
    return MESSAGE_INATTENDU;
  }

  const statut = erreur.status;
  if (STATUTS_INJOIGNABLE.has(statut)) {
    return surcharges[statut] ?? MESSAGE_SERVEUR_INJOIGNABLE;
  }

  const lue = lireErreurApi(erreur);
  const code = lue.code as CodeErreur | undefined;

  if (code && surcharges[code]) {
    return surcharges[code];
  }
  if (surcharges[statut]) {
    return surcharges[statut];
  }
  if (code && MESSAGES_PAR_CODE[code]) {
    return MESSAGES_PAR_CODE[code];
  }
  if (texteFourniParApi(erreur)) {
    return lue.message;
  }
  if (MESSAGES_PAR_STATUT[statut]) {
    return MESSAGES_PAR_STATUT[statut];
  }
  if (statut >= 500) {
    return MESSAGES_PAR_STATUT[500];
  }
  return MESSAGE_INATTENDU;
}

function texteFourniParApi(erreur: HttpErrorResponse): boolean {
  const corps: unknown = erreur.error;
  if (!corps || typeof corps !== 'object') {
    return false;
  }
  const donnees = corps as Record<string, unknown>;
  return typeof donnees['detail'] === 'string' || typeof donnees['message'] === 'string';
}
