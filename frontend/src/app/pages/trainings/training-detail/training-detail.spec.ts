import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import {
  MESSAGES_PAR_CODE,
  MESSAGE_SERVEUR_INJOIGNABLE,
} from '../../../services/api-error-messages';
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

  it("affiche l'état « Training not found » si la formation est introuvable (404)", () => {
    const composant = TestBed.createComponent(TrainingDetail).componentInstance;

    http
      .expectOne('/api/formations/3')
      .flush(
        { detail: "Aucune formation avec l'identifiant 3.", code: 'RESSOURCE_INTROUVABLE' },
        { status: 404, statusText: 'Not Found' },
      );

    expect(composant.formation()).toBeNull();
    // IP-45 : un 404 n'affiche plus le detail (français) de l'API mais un état dédié.
    expect(composant.introuvable()).toBe(true);
    expect(composant.erreur()).toBeNull();
    expect(composant.chargement()).toBe(false);
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

  it('désactive « Delete » pendant la suppression et affiche l’erreur 409 traduite', () => {
    const fixture = TestBed.createComponent(TrainingDetail);
    const composant = fixture.componentInstance;
    http.expectOne('/api/formations/3').flush({
      id: 3,
      intitule: 'Spring Boot',
      description: null,
      duree: 30,
      capaciteMax: 20,
      dateDebut: null,
      dateFin: null,
      statut: 'OUVERTE',
      placesRestantes: 19,
    });

    composant.supprimer();
    fixture.detectChanges();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.danger',
    ) as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
    expect(bouton.textContent).toContain('Deleting');

    http
      .expectOne('/api/formations/3')
      .flush(
        { detail: 'La formation a des inscriptions.', code: 'CAPACITE_INSUFFISANTE' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();

    expect(composant.suppressionEnCours()).toBe(false);
    expect(bouton.disabled).toBe(false);
    expect(composant.erreur()).toBe(MESSAGES_PAR_CODE.CAPACITE_INSUFFISANTE);
    // La formation reste affichée : l'erreur ne vide pas la page.
    expect(composant.formation()?.intitule).toBe('Spring Boot');
  });

  it('propose Retry après une panne réseau et recharge la formation', () => {
    const fixture = TestBed.createComponent(TrainingDetail);
    const composant = fixture.componentInstance;
    http.expectOne('/api/formations/3').error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(composant.erreur()).toBe(MESSAGE_SERVEUR_INJOIGNABLE);
    expect(composant.introuvable()).toBe(false);

    composant.charger();
    expect(composant.chargement()).toBe(true);
    http.expectOne('/api/formations/3').flush(null, { status: 404, statusText: 'Not Found' });
    expect(composant.introuvable()).toBe(true);
    expect(composant.erreur()).toBeNull();
  });
});
