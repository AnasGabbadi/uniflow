import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { TrainingDetail } from './training-detail';

@Component({ selector: 'app-vide', template: '' })
class Vide {}

describe('TrainingDetail', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'trainings', component: Vide }]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '3' }) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge la formation demandée', () => {
    const composant = TestBed.createComponent(TrainingDetail).componentInstance;

    http.expectOne('/api/formations/3').flush({
      id: 3,
      intitule: 'Spring Boot',
      description: 'Niveau avancé',
      duree: 30,
      capaciteMax: 20,
      dateDebut: '2027-01-10',
      dateFin: '2027-01-20',
      statut: 'OUVERTE',
      placesRestantes: 18,
    });

    expect(composant.formation()?.intitule).toBe('Spring Boot');
    expect(composant.messageSuppression()).toContain('Spring Boot');
    expect(composant.chargement()).toBe(false);
  });

  it('affiche une erreur si la formation est introuvable', () => {
    const composant = TestBed.createComponent(TrainingDetail).componentInstance;

    http
      .expectOne('/api/formations/3')
      .flush(
        { detail: "Aucune formation avec l'identifiant 3.", code: 'RESSOURCE_INTROUVABLE' },
        { status: 404, statusText: 'Not Found' },
      );

    expect(composant.formation()).toBeNull();
    expect(composant.erreur()).toContain('Aucune formation');
  });

  it('supprime la formation puis revient à la liste', async () => {
    const fixture = TestBed.createComponent(TrainingDetail);
    const composant = fixture.componentInstance;
    const router = TestBed.inject(Router);
    http.expectOne('/api/formations/3').flush({
      id: 3,
      intitule: 'Spring Boot',
      description: null,
      duree: 30,
      capaciteMax: 20,
      dateDebut: null,
      dateFin: null,
      statut: 'OUVERTE',
      placesRestantes: 20,
    });

    composant.confirmationOuverte.set(true);
    composant.supprimer();

    const requete = http.expectOne('/api/formations/3');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(composant.confirmationOuverte()).toBe(false);
    expect(router.url).toBe('/trainings');
  });
});
