
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Etudiant } from '../../models/etudiant.model';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './students.html',
  styleUrl: './students.scss',
})
export class Students implements OnInit {
  private readonly etudiantService = inject(EtudiantService);
  private readonly router = inject(Router);

  readonly students = signal<Etudiant[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.etudiantService
      .lister()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (liste) => this.students.set(liste),
        error: (err) => {
          console.error('[Students] load failed', err);
          this.error.set('Impossible de charger la liste des étudiants.');
        },
      });
  }

  ajouter(): void {
    void this.router.navigate(['/students/new']);
  }

  modifier(id: number): void {
    void this.router.navigate(['/students', id, 'edit']);
  }
}
