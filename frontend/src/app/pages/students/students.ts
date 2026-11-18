
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { Etudiant, EtudiantRequest } from '../../models/etudiant.model';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './students.html',
  styleUrl: './students.scss',
})
export class Students implements OnInit {
  private readonly etudiantService = inject(EtudiantService);

  readonly students = signal<Etudiant[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly formulaireVisible = signal(false);
  readonly saving = signal(false);
  readonly success = signal<string | null>(null);

  nouveauEtudiant: EtudiantRequest = this.formulaireVide();

  private formulaireVide(): EtudiantRequest {
    return {
      nom: '',
      prenom: '',
      email: '',
      telephone: null,
      adresse: null,
      dateNaissance: null,
    };
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.etudiantService.lister()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (liste) => this.students.set(liste),
        error: (err) => {
          console.error('[Students] load failed', err);
          this.error.set('Impossible de charger la liste des étudiants.');
        },
      });
  }

  afficherFormulaire(): void {
    this.nouveauEtudiant = this.formulaireVide();
    this.success.set(null);
    this.formulaireVisible.set(true);
  }

  annuler(): void {
    this.formulaireVisible.set(false);
    this.nouveauEtudiant = this.formulaireVide();
  }

  creer(): void {
    this.saving.set(true);
    this.success.set(null);

    const donnees: EtudiantRequest = {
      nom: this.nouveauEtudiant.nom.trim(),
      prenom: this.nouveauEtudiant.prenom.trim(),
      email: this.nouveauEtudiant.email.trim(),
      telephone: this.nouveauEtudiant.telephone?.trim() || null,
      adresse: this.nouveauEtudiant.adresse?.trim() || null,
      dateNaissance: this.nouveauEtudiant.dateNaissance || null,
    };

    this.etudiantService.creer(donnees)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (etudiant) => {
          this.students.update(liste => [...liste, etudiant]);
          this.formulaireVisible.set(false);
          this.nouveauEtudiant = this.formulaireVide();
          this.success.set('Étudiant ajouté avec succès !');
        },
        error: (err) => {
          console.error('[Students] create failed', err);
          this.error.set("Impossible d'ajouter cet étudiant. Vérifie les informations.");
        },
      });
  }
}
