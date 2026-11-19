export type StatutFormation = 'OUVERTE' | 'FERMEE' | 'ARCHIVEE';

export const LIBELLES_STATUT_FORMATION: Record<StatutFormation, string> = {
  OUVERTE: 'Open',
  FERMEE: 'Closed',
  ARCHIVEE: 'Archived',
};

export interface Formation {
  id: number;
  intitule: string;
  description: string | null;
  duree: number;
  capaciteMax: number;
  dateDebut: string | null;
  dateFin: string | null;
  statut: StatutFormation;
  placesRestantes: number;
}

export interface FormationRequest {
  intitule: string;
  description: string | null;
  duree: number;
  capaciteMax: number;
  dateDebut: string;
  dateFin: string;
  statut?: StatutFormation;
}

export type FormationResume = Pick<Formation, 'id' | 'intitule'>;
