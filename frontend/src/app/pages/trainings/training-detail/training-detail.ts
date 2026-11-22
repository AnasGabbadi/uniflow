import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ConfirmationDialog } from '../../../components/shared/confirmation-dialog/confirmation-dialog';
import { UiAlert } from '../../../components/shared/ui-alert/ui-alert';
import { UiLoading } from '../../../components/shared/ui-loading/ui-loading';
import { Formation, LIBELLES_STATUT_FORMATION } from '../../../models/formation.model';
import { messageErreurApi } from '../../../services/api-error-messages';
import { FormationService } from '../../../services/formation.service';

@Component({
  selector: 'app-training-detail',
  imports: [DatePipe, RouterLink, ConfirmationDialog, UiAlert, UiLoading],
  templateUrl: './training-detail.html',
  styleUrl: './training-detail.scss',
})
export class TrainingDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formationService = inject(FormationService);

  readonly libellesStatut = LIBELLES_STATUT_FORMATION;
  readonly formation = signal<Formation | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);
  /** Vrai si l'API répond 404 : la page affiche « Training not found » au lieu d'une zone vide. */
  readonly introuvable = signal(false);
  readonly confirmationOuverte = signal(false);
  /** Vrai pendant la requête de suppression : le bouton « Delete » est désactivé (pas de double clic). */
  readonly suppressionEnCours = signal(false);

  readonly messageSuppression = computed(() => {
    const formation = this.formation();
    return formation
      ? `Delete the training "${formation.intitule}"? Its enrollments will also be deleted.`
      : '';
  });

  constructor() {
    this.charger();
  }

  /** Charge la formation ; rappelée par le bouton « Retry » après une erreur. */
  charger(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.chargement.set(true);
    this.erreur.set(null);
    this.introuvable.set(false);
    this.formationService.obtenir(id).subscribe({
      next: (formation) => {
        this.formation.set(formation);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        if (erreur instanceof HttpErrorResponse && erreur.status === 404) {
          this.introuvable.set(true);
        } else {
          this.erreur.set(messageErreurApi(erreur));
        }
        this.chargement.set(false);
      },
    });
  }

  supprimer(): void {
    const formation = this.formation();
    this.confirmationOuverte.set(false);
    if (!formation) {
      return;
    }
    this.erreur.set(null);
    this.suppressionEnCours.set(true);
    this.formationService.supprimer(formation.id).subscribe({
      next: () => {
        this.suppressionEnCours.set(false);
        void this.router.navigate(['/trainings']);
      },
      error: (erreur: unknown) => {
        this.erreur.set(messageErreurApi(erreur));
        this.suppressionEnCours.set(false);
      },
    });
  }
}
