import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Etudiant } from '../models/etudiant.model';
import { Formation, FormationRequest } from '../models/formation.model';

@Injectable({ providedIn: 'root' })
export class FormationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/formations`;

  lister(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.url);
  }

  obtenir(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.url}/${id}`);
  }

  creer(formation: FormationRequest): Observable<Formation> {
    return this.http.post<Formation>(this.url, formation);
  }

  modifier(id: number, formation: FormationRequest): Observable<Formation> {
    return this.http.put<Formation>(`${this.url}/${id}`, formation);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Étudiants dont l'inscription à la formation est EN_ATTENTE ou VALIDEE. */
  listerEtudiants(id: number): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`${this.url}/${id}/etudiants`);
  }
}
