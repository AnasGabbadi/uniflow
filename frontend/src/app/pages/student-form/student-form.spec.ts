import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { StudentForm } from './student-form';

@Component({ selector: 'app-vide', template: '' })
class Vide {}

const ROUTES = [
  { path: 'students', component: Vide },
  { path: 'students/:id', component: Vide },
];

function creerComposant(id: string | null): StudentForm {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(ROUTES),
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            paramMap: convertToParamMap(id ? { id } : {}),
          },
        },
      },
    ],
  });

  return TestBed.createComponent(StudentForm).componentInstance;
}

function remplir(composant: StudentForm): void {
  composant.form.setValue({
    nom: '  El Azami  ',
    prenom: '  Youssef  ',
    email: 'Youssef@Example.com',
    telephone: '0612345678',
    adresse: 'Fès',
    dateNaissance: '2000-01-15',
  });
}

describe('StudentForm', () => {
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  describe('création', () => {
    let composant: StudentForm;
    let http: HttpTestingController;

    beforeEach(() => {
      composant = creerComposant(null);
      http = TestBed.inject(HttpTestingController);
    });

    it("n'appelle pas l'API si le formulaire est invalide", () => {
      composant.enregistrer();

      http.expectNone('/api/etudiants');

      expect(composant.modeEdition).toBe(false);
      expect(composant.messageErreur('nom')).toBe('This field is required.');
    });

    it('refuse une adresse email invalide', () => {
      remplir(composant);
      composant.form.controls.email.setValue('email-invalide');
      composant.form.controls.email.markAsTouched();

      expect(composant.form.invalid).toBe(true);
      expect(composant.messageErreur('email')).toBe('Please enter a valid email address.');

      http.expectNone('/api/etudiants');
    });

    it('refuse une date de naissance dans le futur', () => {
      remplir(composant);

      composant.form.controls.dateNaissance.setValue('2099-01-01');
      composant.form.controls.dateNaissance.markAsTouched();

      expect(composant.messageErreur('dateNaissance')).toBe('Date of birth must be in the past.');

      http.expectNone('/api/etudiants');
    });

    it('envoie les données nettoyées en POST', () => {
      remplir(composant);
      composant.enregistrer();

      const requete = http.expectOne('/api/etudiants');

      expect(requete.request.method).toBe('POST');
      expect(requete.request.body).toEqual({
        nom: 'El Azami',
        prenom: 'Youssef',
        email: 'youssef@example.com',
        telephone: '0612345678',
        adresse: 'Fès',
        dateNaissance: '2000-01-15',
      });

      expect(composant.enCours()).toBe(true);

      requete.flush({
        id: 1,
        ...requete.request.body,
      });

      expect(composant.enCours()).toBe(false);
    });

    it("affiche le conflit d'email renvoyé par l'API", () => {
      remplir(composant);
      composant.enregistrer();

      http.expectOne('/api/etudiants').flush(
        {
          status: 409,
          message: 'Cette adresse email est déjà utilisée.',
        },
        {
          status: 409,
          statusText: 'Conflict',
        },
      );

      expect(composant.erreurServeur()).toContain('email');
      expect(composant.enCours()).toBe(false);
    });

    it('affiche les erreurs de validation du serveur', () => {
      remplir(composant);
      composant.enregistrer();

      http.expectOne('/api/etudiants').flush(
        {
          status: 400,
          message: 'Données invalides',
          erreurs: {
            email: 'Format email incorrect.',
          },
        },
        {
          status: 400,
          statusText: 'Bad Request',
        },
      );

      expect(composant.messageErreur('email')).toBe('Format email incorrect.');
    });
  });

  describe('modification', () => {
    it('charge un étudiant puis envoie un PUT', () => {
      const composant = creerComposant('3');
      const http = TestBed.inject(HttpTestingController);

      http.expectOne('/api/etudiants/3').flush({
        id: 3,
        nom: 'El Azami',
        prenom: 'Youssef',
        email: 'youssef@example.com',
        telephone: '0612345678',
        adresse: 'Fès',
        dateNaissance: '2000-01-15',
      });

      expect(composant.modeEdition).toBe(true);
      expect(composant.form.controls.nom.value).toBe('El Azami');
      expect(composant.form.controls.email.value).toBe('youssef@example.com');

      composant.form.controls.nom.setValue('El Hassani');
      composant.enregistrer();

      const requete = http.expectOne('/api/etudiants/3');

      expect(requete.request.method).toBe('PUT');
      expect(requete.request.body.nom).toBe('El Hassani');

      requete.flush({
        id: 3,
        ...requete.request.body,
      });

      expect(composant.enCours()).toBe(false);
    });
  });
});
