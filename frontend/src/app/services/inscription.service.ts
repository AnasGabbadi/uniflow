import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  Inscription,
  InscriptionFiltre,
  InscriptionRequest,
  StatutInscription,
} from '../models/inscription.model';

@Injectable({ providedIn: 'root' })
export class InscriptionService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/inscriptions`;

  lister(filtre: InscriptionFiltre = {}): Observable<Inscription[]> {
    let params = new HttpParams();
    if (filtre.etudiantId !== undefined) {
      params = params.set('etudiantId', filtre.etudiantId);
    }
    if (filtre.formationId !== undefined) {
      params = params.set('formationId', filtre.formationId);
    }
    if (filtre.statut) {
      params = params.set('statut', filtre.statut);
    }
    return this.http.get<Inscription[]>(this.url, { params });
  }

  obtenir(id: number): Observable<Inscription> {
    return this.http.get<Inscription>(`${this.url}/${id}`);
  }

  creer(inscription: InscriptionRequest): Observable<Inscription> {
    return this.http.post<Inscription>(this.url, inscription);
  }

  changerStatut(id: number, statut: StatutInscription): Observable<Inscription> {
    return this.http.patch<Inscription>(`${this.url}/${id}/statut`, { statut });
  }
}
