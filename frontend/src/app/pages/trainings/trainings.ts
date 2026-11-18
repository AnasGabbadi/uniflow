import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ConfirmationDialog } from '../../components/shared/confirmation-dialog/confirmation-dialog';
import { UiButton } from '../../components/shared/ui-button/ui-button';
import { Formation, LIBELLES_STATUT_FORMATION } from '../../models/formation.model';
import { lireErreurApi } from '../../services/api-error.util';
import { FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-trainings',
  imports: [DatePipe, RouterLink, UiButton, ConfirmationDialog],
  templateUrl: './trainings.html',
  styleUrl: './trainings.scss',
})
export class Trainings {
  private readonly formationService = inject(FormationService);
  private readonly router = inject(Router);

  readonly libellesStatut = LIBELLES_STATUT_FORMATION;
  readonly formations = signal<Formation[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly aSupprimer = signal<Formation | null>(null);

  readonly messageSuppression = computed(() => {
    const formation = this.aSupprimer();
    return formation
      ? `Delete the training "${formation.intitule}"? Its enrollments will also be deleted.`
      : '';
  });

  constructor() {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.erreur.set(null);
    this.formationService.lister().subscribe({
      next: (formations) => {
        this.formations.set(formations);
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.erreur.set(lireErreurApi(erreur).message);
        this.chargement.set(false);
      },
    });
  }

  creer(): void {
    void this.router.navigate(['/trainings/new']);
  }

  demanderSuppression(formation: Formation): void {
    this.aSupprimer.set(formation);
  }

  annulerSuppression(): void {
    this.aSupprimer.set(null);
  }

  confirmerSuppression(): void {
    const formation = this.aSupprimer();
    if (!formation) {
      return;
    }
    this.aSupprimer.set(null);
    this.erreur.set(null);
    this.formationService.supprimer(formation.id).subscribe({
      next: () => this.formations.update((liste) => liste.filter((f) => f.id !== formation.id)),
      error: (erreur: unknown) => this.erreur.set(lireErreurApi(erreur).message),
    });
  }
}
