import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationDialog } from '../../components/shared/confirmation-dialog/confirmation-dialog';
import { UiAlert } from '../../components/shared/ui-alert/ui-alert';
import { UiLoading } from '../../components/shared/ui-loading/ui-loading';
import { Etudiant } from '../../models/etudiant.model';
import { Formation } from '../../models/formation.model';
import { Inscription, InscriptionRequest, StatutInscription } from '../../models/inscription.model';
import { messageErreurApi } from '../../services/api-error-messages';
import { EtudiantService } from '../../services/etudiant.service';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';

@Component({
  selector: 'app-enrollments',
  imports: [DatePipe, FormsModule, ConfirmationDialog, UiAlert, UiLoading],
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
  /** Vrai si le chargement de la liste a échoué : ni tableau ni état vide, seulement l'alerte + Retry. */
  readonly chargementEchoue = signal(false);
  /** Vrai pendant la requête d'annulation : les boutons « Cancel enrollment » sont désactivés. */
  readonly annulationEnCours = signal(false);

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
    this.chargementEchoue.set(false);

    this.inscriptionService.lister().subscribe({
      next: (inscriptions) => {
        this.inscriptions.set(inscriptions);
        this.chargement.set(false);
      },
      error: (err: unknown) => {
        this.erreur.set(messageErreurApi(err));
        this.chargementEchoue.set(true);
        this.chargement.set(false);
      },
    });

    // Listes des sélecteurs : leur échec n'écrase pas l'erreur du chargement principal.
    this.etudiantService.lister().subscribe({
      next: (etudiants) => this.etudiants.set(etudiants),
      error: (err: unknown) => this.erreurSecondaire(err),
    });

    this.formationService.lister().subscribe({
      next: (formations) => this.formations.set(formations),
      error: (err: unknown) => this.erreurSecondaire(err),
    });
  }

  private erreurSecondaire(err: unknown): void {
    if (!this.erreur()) {
      this.erreur.set(messageErreurApi(err));
    }
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
        // INSCRIPTION_EN_DOUBLE, FORMATION_COMPLETE, FORMATION_NON_OUVERTE : textes centralisés.
        this.erreur.set(messageErreurApi(err));
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
    this.annulationEnCours.set(true);

    this.inscriptionService.changerStatut(inscription.id, 'ANNULEE').subscribe({
      next: (miseAJour) => {
        this.inscriptions.update((liste) =>
          liste.map((item) => (item.id === miseAJour.id ? miseAJour : item)),
        );
        this.succes.set('Enrollment cancelled successfully!');
        this.annulationEnCours.set(false);
      },
      error: (err: unknown) => {
        this.erreur.set(messageErreurApi(err));
        this.annulationEnCours.set(false);
      },
    });
  }
}
