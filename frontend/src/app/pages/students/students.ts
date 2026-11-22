import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { UiAlert } from '../../components/shared/ui-alert/ui-alert';
import { UiLoading } from '../../components/shared/ui-loading/ui-loading';
import { Etudiant } from '../../models/etudiant.model';
import { Formation } from '../../models/formation.model';
import { messageErreurApi } from '../../services/api-error-messages';
import { EtudiantService } from '../../services/etudiant.service';
import { FormationService } from '../../services/formation.service';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [RouterLink, UiAlert, UiLoading],
  templateUrl: './students.html',
  styleUrl: './students.scss',
})
export class Students implements OnInit {
  private readonly etudiantService = inject(EtudiantService);
  private readonly formationService = inject(FormationService);
  private readonly router = inject(Router);

  readonly allStudents = signal<Etudiant[]>([]);
  readonly formationStudents = signal<Etudiant[]>([]);
  readonly formations = signal<Formation[]>([]);

  readonly searchTerm = signal('');
  readonly selectedFormationId = signal<number | null>(null);

  readonly loading = signal(true);
  readonly filterLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  /** Échec du chargement de la liste des étudiants. */
  readonly chargementEchoue = signal(false);
  /** Échec du chargement des étudiants de la formation sélectionnée. */
  readonly filtreEchoue = signal(false);
  /** En cas d'échec, ni tableau ni état vide : seulement l'alerte + Retry. */
  readonly echecChargement = computed(() => this.chargementEchoue() || this.filtreEchoue());

  readonly students = computed(() => {
    const source =
      this.selectedFormationId() === null ? this.allStudents() : this.formationStudents();

    const terme = this.normaliser(this.searchTerm().trim());

    if (!terme) {
      return source;
    }

    return source.filter((student) => {
      const nomComplet = this.normaliser(
        `${student.nom} ${student.prenom} ${student.prenom} ${student.nom}`,
      );

      return nomComplet.includes(terme);
    });
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.chargementEchoue.set(false);

    this.etudiantService
      .lister()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (liste) => this.allStudents.set(liste),
        error: (err: unknown) => {
          this.error.set(messageErreurApi(err));
          this.chargementEchoue.set(true);
        },
      });

    this.formationService.lister().subscribe({
      next: (liste) => this.formations.set(liste),
      // N'écrase pas l'erreur du chargement principal (liste des étudiants).
      error: (err: unknown) => {
        if (!this.error()) {
          this.error.set(messageErreurApi(err));
        }
      },
    });
  }

  /** Bouton « Retry » : recharge les listes, puis le filtre par formation s'il est actif. */
  reessayer(): void {
    this.load();
    const formationId = this.selectedFormationId();
    if (formationId !== null) {
      this.changerFormation(String(formationId));
    }
  }

  rechercher(terme: string): void {
    this.searchTerm.set(terme);
  }

  changerFormation(valeur: string): void {
    const formationId = valeur ? Number(valeur) : null;

    this.error.set(null);
    this.filtreEchoue.set(false);
    this.selectedFormationId.set(formationId);

    if (formationId === null) {
      this.filterLoading.set(false);
      this.formationStudents.set([]);
      return;
    }

    this.filterLoading.set(true);
    this.formationStudents.set([]);

    this.formationService
      .listerEtudiants(formationId)
      .pipe(finalize(() => this.filterLoading.set(false)))
      .subscribe({
        next: (liste) => {
          if (this.selectedFormationId() === formationId) {
            this.formationStudents.set(liste);
          }
        },
        error: (err: unknown) => {
          if (this.selectedFormationId() === formationId) {
            this.error.set(messageErreurApi(err));
            this.filtreEchoue.set(true);
          }
        },
      });
  }

  private normaliser(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  ajouter(): void {
    void this.router.navigate(['/students/new']);
  }

  modifier(id: number): void {
    void this.router.navigate(['/students', id, 'edit']);
  }
}
