import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { TrainingForm } from './training-form';

@Component({ selector: 'app-vide', template: '' })
class Vide {}

const ROUTES = [
  { path: 'trainings', component: Vide },
  { path: 'trainings/:id', component: Vide },
];

function creerComposant(id: string | null): TrainingForm {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(ROUTES),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } },
      },
    ],
  });
  return TestBed.createComponent(TrainingForm).componentInstance;
}

function remplir(composant: TrainingForm): void {
  composant.form.setValue({
    intitule: '  DevOps  ',
    description: '',
    duree: 40,
    capaciteMax: 25,
    dateDebut: '2026-12-01',
    dateFin: '2026-12-12',
    statut: 'OUVERTE',
  });
}

describe('TrainingForm', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  describe('création', () => {
    let composant: TrainingForm;
    let http: HttpTestingController;

    beforeEach(() => {
      composant = creerComposant(null);
      http = TestBed.inject(HttpTestingController);
    });

    it("n'appelle pas l'API si le formulaire est invalide", () => {
      composant.enregistrer();

      http.expectNone('/api/formations');
      expect(composant.modeEdition).toBe(false);
      expect(composant.messageErreur('intitule')).toBe('This field is required.');
    });

    it('refuse une date de fin avant la date de début', () => {
      remplir(composant);
      composant.form.controls.dateFin.setValue('2026-11-01');
      composant.form.markAllAsTouched();

      expect(composant.form.hasError('periode')).toBe(true);
      expect(composant.messageErreur('dateFin')).toBe('End date cannot be before start date.');
    });

    it('refuse une durée nulle ou décimale', () => {
      remplir(composant);
      composant.form.controls.duree.setValue(0);
      composant.form.controls.duree.markAsTouched();
      expect(composant.messageErreur('duree')).toBe('Must be greater than 0.');

      composant.form.controls.duree.setValue(2.5);
      expect(composant.messageErreur('duree')).toBe('Must be a whole number.');
    });

    it('envoie la formation nettoyée en POST', () => {
      remplir(composant);
      composant.enregistrer();

      const requete = http.expectOne('/api/formations');
      expect(requete.request.method).toBe('POST');
      expect(requete.request.body).toEqual({
        intitule: 'DevOps',
        description: null,
        duree: 40,
        capaciteMax: 25,
        dateDebut: '2026-12-01',
        dateFin: '2026-12-12',
        statut: 'OUVERTE',
      });
      expect(composant.enCours()).toBe(true);

      requete.flush({ ...requete.request.body, id: 5, placesRestantes: 25 });
      expect(composant.enCours()).toBe(false);
    });

    it("affiche le conflit renvoyé par l'API", () => {
      remplir(composant);
      composant.enregistrer();

      http
        .expectOne('/api/formations')
        .flush(
          { status: 409, message: "Une formation avec l'intitulé 'DevOps' existe déjà" },
          { status: 409, statusText: 'Conflict' },
        );

      expect(composant.erreurServeur()).toContain('existe déjà');
      expect(composant.enCours()).toBe(false);
    });

    it('place les erreurs de champ du serveur sous le bon champ', () => {
      remplir(composant);
      composant.enregistrer();

      http.expectOne('/api/formations').flush(
        {
          status: 400,
          message: 'Données invalides',
          erreurs: { periodeValide: 'La date de fin ne peut pas précéder la date de début' },
        },
        { status: 400, statusText: 'Bad Request' },
      );

      expect(composant.messageErreur('dateFin')).toBe(
        'La date de fin ne peut pas précéder la date de début',
      );
    });
  });

  describe('modification', () => {
    it('charge la formation puis envoie un PUT', () => {
      const composant = creerComposant('3');
      const http = TestBed.inject(HttpTestingController);

      http.expectOne('/api/formations/3').flush({
        id: 3,
        intitule: 'Spring Boot',
        description: null,
        duree: 30,
        capaciteMax: 20,
        dateDebut: '2027-01-10',
        dateFin: '2027-01-20',
        statut: 'FERMEE',
        placesRestantes: 18,
      });

      expect(composant.modeEdition).toBe(true);
      expect(composant.form.controls.intitule.value).toBe('Spring Boot');
      expect(composant.form.controls.statut.value).toBe('FERMEE');

      composant.form.controls.capaciteMax.setValue(30);
      composant.enregistrer();

      const requete = http.expectOne('/api/formations/3');
      expect(requete.request.method).toBe('PUT');
      expect(requete.request.body.capaciteMax).toBe(30);
      requete.flush({ ...requete.request.body, id: 3, placesRestantes: 28 });
    });
  });
});
