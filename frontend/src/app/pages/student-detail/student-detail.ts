import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Etudiant } from '../../models/etudiant.model';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './student-detail.html',
  styleUrl: './student-detail.scss',
})
export class StudentDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly etudiantService = inject(EtudiantService);

  readonly student = signal<Etudiant | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id < 1) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.etudiantService.obtenir(id).subscribe({
      next: (etudiant) => {
        this.student.set(etudiant);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set("Impossible de charger les détails de l'étudiant.");
        }
        this.loading.set(false);
      },
    });
  }
}
