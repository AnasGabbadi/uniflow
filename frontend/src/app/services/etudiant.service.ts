import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Etudiant, EtudiantRequest } from '../models/etudiant.model';

@Injectable({ providedIn: 'root' })
export class EtudiantService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/etudiants`;

  /** Liste les étudiants ; `recherche` filtre sur le nom ou le prénom. */
  lister(recherche?: string): Observable<Etudiant[]> {
    let params = new HttpParams();
    const terme = recherche?.trim();
    if (terme) {
      params = params.set('recherche', terme);
    }
    return this.http.get<Etudiant[]>(this.url, { params });
  }

  obtenir(id: number): Observable<Etudiant> {
    return this.http.get<Etudiant>(`${this.url}/${id}`);
  }

  creer(etudiant: EtudiantRequest): Observable<Etudiant> {
    return this.http.post<Etudiant>(this.url, etudiant);
  }

  modifier(id: number, etudiant: EtudiantRequest): Observable<Etudiant> {
    return this.http.put<Etudiant>(`${this.url}/${id}`, etudiant);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
