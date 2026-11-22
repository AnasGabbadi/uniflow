import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Etudiant } from '../models/etudiant.model';
import { Formation, FormationRequest } from '../models/formation.model';
import { FormationService } from './formation.service';

describe('FormationService', () => {
  let service: FormationService;
  let http: HttpTestingController;

  const devops: Formation = {
    id: 3,
    intitule: 'DevOps',
    description: null,
    duree: 40,
    capaciteMax: 25,
    dateDebut: '2026-12-01',
    dateFin: '2026-12-12',
    statut: 'OUVERTE',
    placesRestantes: 25,
  };

  const requete: FormationRequest = {
    intitule: 'DevOps',
    description: null,
    duree: 40,
    capaciteMax: 25,
    dateDebut: '2026-12-01',
    dateFin: '2026-12-12',
    statut: 'OUVERTE',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FormationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les formations avec GET /api/formations', () => {
    let resultat: Formation[] | undefined;
    service.lister().subscribe((liste) => (resultat = liste));

    const appel = http.expectOne('/api/formations');
    expect(appel.request.method).toBe('GET');
    appel.flush([devops]);

    expect(resultat).toEqual([devops]);
  });

  it('lit une formation avec GET /api/formations/{id}', () => {
    let resultat: Formation | undefined;
    service.obtenir(3).subscribe((f) => (resultat = f));

    const appel = http.expectOne('/api/formations/3');
    expect(appel.request.method).toBe('GET');
    appel.flush(devops);

    expect(resultat?.intitule).toBe('DevOps');
  });

  it('crée une formation avec POST et le corps du formulaire', () => {
    let resultat: Formation | undefined;
    service.creer(requete).subscribe((f) => (resultat = f));

    const appel = http.expectOne('/api/formations');
    expect(appel.request.method).toBe('POST');
    expect(appel.request.body).toEqual(requete);
    appel.flush(devops, { status: 201, statusText: 'Created' });

    expect(resultat?.id).toBe(3);
  });

  it('modifie une formation avec PUT /api/formations/{id}', () => {
    service.modifier(3, { ...requete, capaciteMax: 30 }).subscribe();

    const appel = http.expectOne('/api/formations/3');
    expect(appel.request.method).toBe('PUT');
    expect(appel.request.body.capaciteMax).toBe(30);
    appel.flush({ ...devops, capaciteMax: 30 });
  });

  it('supprime une formation avec DELETE /api/formations/{id}', () => {
    let termine = false;
    service.supprimer(3).subscribe({ complete: () => (termine = true) });

    const appel = http.expectOne('/api/formations/3');
    expect(appel.request.method).toBe('DELETE');
    appel.flush(null, { status: 204, statusText: 'No Content' });

    expect(termine).toBe(true);
  });

  it('liste les étudiants inscrits avec GET /api/formations/{id}/etudiants', () => {
    const sara: Etudiant = {
      id: 1,
      nom: 'El Amrani',
      prenom: 'Sara',
      email: 'sara@example.ma',
      telephone: null,
      adresse: null,
      dateNaissance: null,
    };
    let resultat: Etudiant[] | undefined;
    service.listerEtudiants(3).subscribe((liste) => (resultat = liste));

    const appel = http.expectOne('/api/formations/3/etudiants');
    expect(appel.request.method).toBe('GET');
    appel.flush([sara]);

    expect(resultat).toEqual([sara]);
  });

  it("transmet l'erreur HTTP à l'appelant", () => {
    let statut: number | undefined;
    service.obtenir(99).subscribe({ error: (e: { status: number }) => (statut = e.status) });

    http
      .expectOne('/api/formations/99')
      .flush({ code: 'RESSOURCE_INTROUVABLE' }, { status: 404, statusText: 'Not Found' });

    expect(statut).toBe(404);
  });
});
