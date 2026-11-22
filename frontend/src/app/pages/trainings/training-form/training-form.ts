import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { UiAlert } from '../../../components/shared/ui-alert/ui-alert';
import { UiFormField } from '../../../components/shared/ui-form-field/ui-form-field';
import { UiLoading } from '../../../components/shared/ui-loading/ui-loading';
import {
  FormationRequest,
  LIBELLES_STATUT_FORMATION,
  StatutFormation,
} from '../../../models/formation.model';
import { messageErreurApi } from '../../../services/api-error-messages';
import { lireErreurApi } from '../../../services/api-error.util';
import { FormationService } from '../../../services/formation.service';

/** La date de fin ne peut pas précéder la date de début (dates ISO comparables en texte). */
export const periodeValide: ValidatorFn = (groupe: AbstractControl): ValidationErrors | null => {
  const debut = groupe.get('dateDebut')?.value as string | null;
  const fin = groupe.get('dateFin')?.value as string | null;
  return debut && fin && fin < debut ? { periode: true } : null;
};

/** Champs renvoyés par l'API qui ne portent pas le nom du champ du formulaire. */
const CHAMPS_API: Record<string, string> = { periodeValide: 'dateFin' };

const NOMBRE_ENTIER = /^\d+$/;

@Component({
  selector: 'app-training-form',
  imports: [ReactiveFormsModule, RouterLink, UiFormField, UiAlert, UiLoading],
  templateUrl: './training-form.html',
  styleUrl: './training-form.scss',
})
export class TrainingForm {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formationService = inject(FormationService);

  readonly statuts = Object.entries(LIBELLES_STATUT_FORMATION) as [StatutFormation, string][];
  readonly id: number | null;
  readonly chargement = signal(false);
  readonly enCours = signal(false);
  readonly erreurServeur = signal<string | null>(null);

  readonly form = this.fb.group(
    {
      intitule: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
      description: this.fb.nonNullable.control('', [Validators.maxLength(5000)]),
      duree: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(NOMBRE_ENTIER),
      ]),
      capaciteMax: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(NOMBRE_ENTIER),
      ]),
      dateDebut: this.fb.nonNullable.control('', [Validators.required]),
      dateFin: this.fb.nonNullable.control('', [Validators.required]),
      statut: this.fb.nonNullable.control<StatutFormation>('OUVERTE'),
    },
    { validators: periodeValide },
  );

  constructor() {
    const parametre = this.route.snapshot.paramMap.get('id');
    this.id = parametre ? Number(parametre) : null;
    if (this.id !== null) {
      this.charger(this.id);
    }
  }

  get modeEdition(): boolean {
    return this.id !== null;
  }

  /** Message à afficher sous un champ, ou null s'il est valide ou pas encore touché. */
  messageErreur(champ: keyof typeof this.form.controls): string | null {
    const controle = this.form.controls[champ];
    if (champ === 'dateFin' && this.form.hasError('periode') && controle.touched) {
      return 'End date cannot be before start date.';
    }
    if (!controle.errors || !controle.touched) {
      return null;
    }
    const erreurs = controle.errors;
    if (erreurs['serveur']) {
      return erreurs['serveur'] as string;
    }
    if (erreurs['required']) {
      return 'This field is required.';
    }
    if (erreurs['maxlength']) {
      return `Maximum ${(erreurs['maxlength'] as { requiredLength: number }).requiredLength} characters.`;
    }
    if (erreurs['pattern']) {
      return 'Must be a whole number.';
    }
    if (erreurs['min']) {
      return 'Must be greater than 0.';
    }
    return 'Invalid value.';
  }

  enregistrer(): void {
    this.erreurServeur.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valeurs = this.form.getRawValue();
    const requete: FormationRequest = {
      intitule: valeurs.intitule.trim(),
      description: valeurs.description.trim() || null,
      duree: Number(valeurs.duree),
      capaciteMax: Number(valeurs.capaciteMax),
      dateDebut: valeurs.dateDebut,
      dateFin: valeurs.dateFin,
      statut: valeurs.statut,
    };

    this.enCours.set(true);
    const appel =
      this.id === null
        ? this.formationService.creer(requete)
        : this.formationService.modifier(this.id, requete);

    appel.subscribe({
      next: (formation) => {
        this.enCours.set(false);
        void this.router.navigate(['/trainings', formation.id]);
      },
      error: (erreur: unknown) => {
        this.enCours.set(false);
        this.afficherErreurServeur(erreur);
      },
    });
  }

  private charger(id: number): void {
    this.chargement.set(true);
    this.formationService.obtenir(id).subscribe({
      next: (formation) => {
        this.form.setValue({
          intitule: formation.intitule,
          description: formation.description ?? '',
          duree: formation.duree,
          capaciteMax: formation.capaciteMax,
          dateDebut: formation.dateDebut ?? '',
          dateFin: formation.dateFin ?? '',
          statut: formation.statut,
        });
        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.chargement.set(false);
        this.erreurServeur.set(messageErreurApi(erreur));
      },
    });
  }

  private afficherErreurServeur(erreur: unknown): void {
    const lue = lireErreurApi(erreur);
    this.erreurServeur.set(messageErreurApi(erreur));
    for (const [champApi, message] of Object.entries(lue.champs)) {
      const nom = CHAMPS_API[champApi] ?? champApi;
      const controle = this.form.get(nom);
      if (controle) {
        controle.setErrors({ ...controle.errors, serveur: message });
        controle.markAsTouched();
      }
    }
  }
}
