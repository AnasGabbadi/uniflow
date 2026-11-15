export type StatutFormation = 'OUVERTE' | 'FERMEE' | 'ARCHIVEE';

export interface Formation {
  id: number;
  intitule: string;
  duree: number;
  capaciteMax: number;
  statut: StatutFormation;
  placesRestantes: number;
}

export interface FormationRequest {
  intitule: string;
  duree: number;
  capaciteMax: number;
  statut?: StatutFormation;
}

export type FormationResume = Pick<Formation, 'id' | 'intitule'>;
