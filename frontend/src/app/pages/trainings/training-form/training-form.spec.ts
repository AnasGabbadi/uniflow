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

  describe('validation', () => {
    let composant: TrainingForm;

    function toucher(champ: keyof TrainingForm['form']['controls']): void {
      composant.form.controls[champ].markAsTouched();
    }

    beforeEach(() => {
      composant = creerComposant(null);
      remplir(composant);
    });

    it('est valide avec des données correctes et le statut OUVERTE par défaut', () => {
      // Contrôle nonNullable : sa valeur initiale (avant remplir) est conservée dans defaultValue.
      expect(composant.form.controls.statut.defaultValue).toBe('OUVERTE');
      expect(composant.form.valid).toBe(true);
    });

    it.each(['intitule', 'dateDebut', 'dateFin'] as const)(
      'rend le champ %s obligatoire',
      (champ) => {
        composant.form.controls[champ].setValue('');
        toucher(champ);

        expect(composant.form.invalid).toBe(true);
        expect(composant.messageErreur(champ)).toBe('This field is required.');
      },
    );

    it.each(['duree', 'capaciteMax'] as const)('rend le champ %s obligatoire', (champ) => {
      composant.form.controls[champ].setValue(null);
      toucher(champ);

      expect(composant.messageErreur(champ)).toBe('This field is required.');
    });

    it("limite l'intitulé à 150 caractères", () => {
      composant.form.controls.intitule.setValue('a'.repeat(150));
      expect(composant.form.controls.intitule.valid).toBe(true);

      composant.form.controls.intitule.setValue('a'.repeat(151));
      toucher('intitule');
      expect(composant.messageErreur('intitule')).toBe('Maximum 150 characters.');
    });

    it('limite la description à 5000 caractères', () => {
      composant.form.controls.description.setValue('a'.repeat(5001));
      toucher('description');

      expect(composant.messageErreur('description')).toBe('Maximum 5000 characters.');
    });

    it.each([
      [0, 'Must be greater than 0.'],
      [-3, 'Must be a whole number.'],
      [12.5, 'Must be a whole number.'],
    ])('refuse une capacité de %s', (valeur, message) => {
      composant.form.controls.capaciteMax.setValue(valeur);
      toucher('capaciteMax');

      expect(composant.form.controls.capaciteMax.invalid).toBe(true);
      expect(composant.messageErreur('capaciteMax')).toBe(message);
    });

    it('refuse une durée négative', () => {
      composant.form.controls.duree.setValue(-1);
      toucher('duree');

      expect(composant.form.controls.duree.invalid).toBe(true);
    });

    it('accepte une date de fin égale à la date de début', () => {
      composant.form.controls.dateFin.setValue('2026-12-01');

      expect(composant.form.hasError('periode')).toBe(false);
      expect(composant.form.valid).toBe(true);
    });

    it("n'affiche le message de période que sous la date de fin touchée", () => {
      composant.form.controls.dateFin.setValue('2026-11-01');

      expect(composant.messageErreur('dateFin')).toBeNull();
      toucher('dateFin');
      expect(composant.messageErreur('dateFin')).toBe('End date cannot be before start date.');
      expect(composant.messageErreur('dateDebut')).toBeNull();
    });

    it("n'appelle pas l'API si la période est invalide", () => {
      composant.form.controls.dateFin.setValue('2026-11-01');
      composant.enregistrer();

      TestBed.inject(HttpTestingController).expectNone('/api/formations');
      expect(composant.form.controls.intitule.touched).toBe(true);
    });

    it('envoie une description non vide nettoyée et le statut choisi', () => {
      composant.form.controls.description.setValue('  Docker et Kubernetes  ');
      composant.form.controls.statut.setValue('FERMEE');
      composant.enregistrer();

      const requete = TestBed.inject(HttpTestingController).expectOne('/api/formations');
      expect(requete.request.body.description).toBe('Docker et Kubernetes');
      expect(requete.request.body.statut).toBe('FERMEE');
      requete.flush({ ...requete.request.body, id: 8, placesRestantes: 25 });
    });
  });
});
