import { Injectable, computed, signal } from '@angular/core';

/**
 * Compte les requêtes HTTP en cours (alimenté par loadingInterceptor).
 * `actif` pilote la barre de progression globale du layout.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly enCours = signal(0);

  readonly requetesEnCours = this.enCours.asReadonly();
  readonly actif = computed(() => this.enCours() > 0);

  demarrer(): void {
    this.enCours.update((n) => n + 1);
  }

  terminer(): void {
    this.enCours.update((n) => Math.max(0, n - 1));
  }
}
