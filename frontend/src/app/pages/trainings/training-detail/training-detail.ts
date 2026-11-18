import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ConfirmationDialog } from '../../../components/shared/confirmation-dialog/confirmation-dialog';
import { Formation, LIBELLES_STATUT_FORMATION } from '../../../models/formation.model';
import { lireErreurApi } from '../../../services/api-error.util';
import { FormationService } from '../../../services/formation.service';

@Component({
  selector: 'app-training-detail',
  imports: [DatePipe, RouterLink, ConfirmationDialog],
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
  readonly confirmationOuverte = signal(false);

  readonly messageSuppression = computed(() => {
    const formation = this.formation();
    return formation
      ? `Delete the training "${formation.intitule}"? Its enrollments will also be deleted.`
      : '';
  });

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.formationService.obtenir(id).subscribe({
      next: (formation) => {
        this.formation.set(formation);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(lireErreurApi(erreur).message);
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
    this.formationService.supprimer(formation.id).subscribe({
      next: () => void this.router.navigate(['/trainings']),
      error: (erreur: unknown) => this.erreur.set(lireErreurApi(erreur).message),
    });
  }
}
