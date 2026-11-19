import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationDialog } from '../../components/shared/confirmation-dialog/confirmation-dialog';
import { Etudiant } from '../../models/etudiant.model';
import { Formation } from '../../models/formation.model';
import { Inscription, InscriptionRequest, StatutInscription } from '../../models/inscription.model';
import { lireErreurApi } from '../../services/api-error.util';
import { EtudiantService } from '../../services/etudiant.service';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';

@Component({
  selector: 'app-enrollments',
  imports: [DatePipe, FormsModule, ConfirmationDialog],
  templateUrl: './enrollments.html',
  styleUrl: './enrollments.scss',
})
export class Enrollments {
  private readonly inscriptionService = inject(InscriptionService);
  private readonly etudiantService = inject(EtudiantService);
  private readonly formationService = inject(FormationService);

  readonly inscriptions = signal<Inscription[]>([]);
  readonly etudiants = signal<Etudiant[]>([]);
  readonly formations = signal<Formation[]>([]);

  readonly chargement = signal(true);
  readonly saving = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly succes = signal<string | null>(null);

  readonly formulaireVisible = signal(false);
  readonly inscriptionAAnnuler = signal<Inscription | null>(null);

  readonly messageConfirmation = computed(() => {
    const inscription = this.inscriptionAAnnuler();

    return inscription
      ? `Cancel the enrollment of ${inscription.etudiant.prenom} ${inscription.etudiant.nom} for "${inscription.formation.intitule}"?`
      : '';
  });

  readonly nouvelleInscription: InscriptionRequest = {
    etudiantId: 0,
    formationId: 0,
  };

  readonly libellesStatut: Record<StatutInscription, string> = {
    EN_ATTENTE: 'Pending',
    VALIDEE: 'Validated',
    ANNULEE: 'Cancelled',
  };

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);

    this.inscriptionService.lister().subscribe({
      next: (inscriptions) => {
        this.inscriptions.set(inscriptions);
        this.chargement.set(false);
      },
      error: (err: unknown) => {
        this.erreur.set(lireErreurApi(err).message);
        this.chargement.set(false);
      },
    });

    this.etudiantService.lister().subscribe({
      next: (etudiants) => this.etudiants.set(etudiants),
      error: (err: unknown) => this.erreur.set(lireErreurApi(err).message),
    });

    this.formationService.lister().subscribe({
      next: (formations) => this.formations.set(formations),
      error: (err: unknown) => this.erreur.set(lireErreurApi(err).message),
    });
  }

  afficherFormulaire(): void {
    this.nouvelleInscription.etudiantId = 0;
    this.nouvelleInscription.formationId = 0;
    this.erreur.set(null);
    this.succes.set(null);
    this.formulaireVisible.set(true);
  }

  annulerFormulaire(): void {
    this.formulaireVisible.set(false);
    this.erreur.set(null);
  }

  creer(): void {
    if (!this.nouvelleInscription.etudiantId || !this.nouvelleInscription.formationId) {
      this.erreur.set('Please select a student and a training.');
      return;
    }

    this.saving.set(true);
    this.erreur.set(null);
    this.succes.set(null);

    this.inscriptionService.creer(this.nouvelleInscription).subscribe({
      next: (inscription) => {
        this.inscriptions.update((liste) => [inscription, ...liste]);
        this.formulaireVisible.set(false);
        this.saving.set(false);
        this.succes.set('Enrollment created successfully!');
        this.nouvelleInscription.etudiantId = 0;
        this.nouvelleInscription.formationId = 0;
      },
      error: (err: unknown) => {
        const resultat = lireErreurApi(err);

        const messages: Record<string, string> = {
          INSCRIPTION_EN_DOUBLE: 'This student is already enrolled in this training.',
          FORMATION_COMPLETE: 'This training has reached its maximum capacity.',
          FORMATION_NON_OUVERTE: 'This training is not open for enrollment.',
        };

        this.erreur.set((resultat.code && messages[resultat.code]) || resultat.message);
        this.saving.set(false);
      },
    });
  }

  demanderAnnulation(inscription: Inscription): void {
    this.inscriptionAAnnuler.set(inscription);
  }

  annulerConfirmation(): void {
    this.inscriptionAAnnuler.set(null);
  }

  confirmerAnnulation(): void {
    const inscription = this.inscriptionAAnnuler();

    if (!inscription) {
      return;
    }

    this.inscriptionAAnnuler.set(null);
    this.erreur.set(null);
    this.succes.set(null);

    this.inscriptionService.changerStatut(inscription.id, 'ANNULEE').subscribe({
      next: (miseAJour) => {
        this.inscriptions.update((liste) =>
          liste.map((item) => (item.id === miseAJour.id ? miseAJour : item)),
        );
        this.succes.set('Enrollment cancelled successfully!');
      },
      error: (err: unknown) => {
        this.erreur.set(lireErreurApi(err).message);
      },
    });
  }
}
