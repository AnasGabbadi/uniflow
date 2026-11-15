export interface Etudiant {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  adresse: string | null;
  dateNaissance: string | null;
}

export type EtudiantRequest = Omit<Etudiant, 'id'>;

export type EtudiantResume = Pick<Etudiant, 'id' | 'nom' | 'prenom' | 'email'>;
