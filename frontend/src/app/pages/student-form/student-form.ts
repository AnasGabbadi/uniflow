import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { UiFormField } from '../../components/shared/ui-form-field/ui-form-field';
import { EtudiantRequest } from '../../models/etudiant.model';
import { lireErreurApi } from '../../services/api-error.util';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-student-form',
  imports: [ReactiveFormsModule, RouterLink, UiFormField],
  templateUrl: './student-form.html',
  styleUrl: './student-form.scss',
})
export class StudentForm {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly etudiantService = inject(EtudiantService);

  readonly id: number | null;
  readonly chargement = signal(false);
  readonly enCours = signal(false);
  readonly erreurServeur = signal<string | null>(null);

  readonly form = this.fb.group({
    nom: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(100),
    ]),
    prenom: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(100),
    ]),
    email: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.email,
      Validators.maxLength(255),
    ]),
    telephone: this.fb.nonNullable.control('', [
      Validators.maxLength(30),
      Validators.pattern(/^[0-9 +\-]*$/),
    ]),
    adresse: this.fb.nonNullable.control('', [
      Validators.maxLength(255),
    ]),
    dateNaissance: this.fb.nonNullable.control('', [
      (control) => {
        const value = control.value as string;
        if (!value) return null;

        const today = new Date();
        const todayISO = [
          today.getFullYear(),
          String(today.getMonth() + 1).padStart(2, '0'),
          String(today.getDate()).padStart(2, '0'),
        ].join('-');

        return value < todayISO ? null : { past: true };
      },
    ]),
  });

  constructor() {
    const parametre = this.route.snapshot.paramMap.get('id');
    this.id = parametre ? Number(parametre) : null;

    if (this.id !== null && Number.isInteger(this.id) && this.id > 0) {
      this.charger(this.id);
    }
  }

  get modeEdition(): boolean {
    return this.id !== null;
  }

  messageErreur(
    champ: keyof typeof this.form.controls,
  ): string | null {
    const controle = this.form.controls[champ];

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

    if (erreurs['email']) {
      return 'Please enter a valid email address.';
    }

    if (erreurs['maxlength']) {
      return `Maximum ${
        (erreurs['maxlength'] as { requiredLength: number }).requiredLength
      } characters.`;
    }

    if (erreurs['pattern']) {
      return 'Invalid phone number.';
    }

    if (erreurs['past']) {
      return 'Date of birth must be in the past.';
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

    const requete: EtudiantRequest = {
      nom: valeurs.nom.trim(),
      prenom: valeurs.prenom.trim(),
      email: valeurs.email.trim().toLowerCase(),
      telephone: valeurs.telephone.trim() || null,
      adresse: valeurs.adresse.trim() || null,
      dateNaissance: valeurs.dateNaissance || null,
    };

    this.enCours.set(true);

    const appel =
      this.id === null
        ? this.etudiantService.creer(requete)
        : this.etudiantService.modifier(this.id, requete);

    appel.subscribe({
      next: () => {
        this.enCours.set(false);
        void this.router.navigate(['/students']);
      },
      error: (erreur: unknown) => {
        this.enCours.set(false);
        this.afficherErreurServeur(erreur);
      },
    });
  }

  private charger(id: number): void {
    this.chargement.set(true);

    this.etudiantService.obtenir(id).subscribe({
      next: (etudiant) => {
        this.form.setValue({
          nom: etudiant.nom,
          prenom: etudiant.prenom,
          email: etudiant.email,
          telephone: etudiant.telephone ?? '',
          adresse: etudiant.adresse ?? '',
          dateNaissance: etudiant.dateNaissance ?? '',
        });

        this.chargement.set(false);
      },
      error: (erreur: unknown) => {
        this.chargement.set(false);
        this.erreurServeur.set(lireErreurApi(erreur).message);
      },
    });
  }

  private afficherErreurServeur(erreur: unknown): void {
    const lue = lireErreurApi(erreur);
    this.erreurServeur.set(lue.message);

    for (const [champ, message] of Object.entries(lue.champs)) {
      const controle = this.form.get(champ);

      if (controle) {
        controle.setErrors({
          ...controle.errors,
          serveur: message,
        });
        controle.markAsTouched();
      }
    }
  }
}
