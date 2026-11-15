import { EtudiantResume } from './etudiant.model';
import { FormationResume } from './formation.model';

export type StatutInscription = 'EN_ATTENTE' | 'VALIDEE' | 'ANNULEE';

export interface Inscription {
  id: number;
  dateInscription: string;
  statut: StatutInscription;
  etudiant: EtudiantResume;
  formation: FormationResume;
}

export interface InscriptionRequest {
  etudiantId: number;
  formationId: number;
}

export interface InscriptionFiltre {
  etudiantId?: number;
  formationId?: number;
  statut?: StatutInscription;
}
