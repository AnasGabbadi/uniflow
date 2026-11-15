export type CodeErreur =
  | 'VALIDATION'
  | 'REQUETE_INVALIDE'
  | 'RESSOURCE_INTROUVABLE'
  | 'EMAIL_DEJA_UTILISE'
  | 'INTITULE_DEJA_UTILISE'
  | 'CAPACITE_INSUFFISANTE'
  | 'INSCRIPTION_EN_DOUBLE'
  | 'FORMATION_COMPLETE'
  | 'FORMATION_NON_OUVERTE'
  | 'TRANSITION_INTERDITE'
  | 'ERREUR_INTERNE';

export interface ErreurChamp {
  champ: string;
  message: string;
}

/** Réponse d'erreur au format Problem Details (RFC 9457), voir docs/api-contract.md. */
export interface ApiError {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: CodeErreur;
  erreurs?: ErreurChamp[];
}
