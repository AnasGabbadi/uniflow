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

  describe('validation', () => {
    let composant: StudentForm;

    /** Date du jour au format AAAA-MM-JJ (heure locale, comme le validateur du formulaire). */
    function aujourdhui(): string {
      const d = new Date();
      return [
        d.getFullYear(),
        String(d.getMonth() + 1).padStart(2, '0'),
        String(d.getDate()).padStart(2, '0'),
      ].join('-');
    }

    function saisir(champ: keyof StudentForm['form']['controls'], valeur: string): void {
      const controle = composant.form.controls[champ];
      controle.setValue(valeur);
      controle.markAsTouched();
    }

    beforeEach(() => {
      composant = creerComposant(null);
      remplir(composant);
    });

    it("n'affiche aucun message tant que le champ n'a pas été touché", () => {
      composant.form.controls.nom.setValue('');

      expect(composant.form.controls.nom.invalid).toBe(true);
      expect(composant.messageErreur('nom')).toBeNull();
    });

    it.each(['nom', 'prenom', 'email'] as const)('rend le champ %s obligatoire', (champ) => {
      saisir(champ, '');

      expect(composant.form.invalid).toBe(true);
      expect(composant.messageErreur(champ)).toBe('This field is required.');
    });

    it('accepte un formulaire avec seulement les champs obligatoires', () => {
      saisir('telephone', '');
      saisir('adresse', '');
      saisir('dateNaissance', '');

      expect(composant.form.valid).toBe(true);
    });

    it.each([
      ['nom', 100],
      ['prenom', 100],
      ['adresse', 255],
      ['telephone', 30],
    ] as const)('limite %s à %i caractères', (champ, max) => {
      const valeur = champ === 'telephone' ? '1'.repeat(max) : 'a'.repeat(max);
      saisir(champ, valeur);
      expect(composant.form.controls[champ].valid).toBe(true);

      saisir(champ, valeur + (champ === 'telephone' ? '1' : 'a'));
      expect(composant.messageErreur(champ)).toBe(`Maximum ${max} characters.`);
    });

    it("limite l'e-mail à 255 caractères", () => {
      const domaine = '@example.com';
      saisir('email', 'a'.repeat(256 - domaine.length) + domaine);

      expect(composant.form.controls.email.hasError('maxlength')).toBe(true);
    });

    it.each(['0612345678', '+212 6 12 34 56 78', '06-12-34-56-78'])(
      'accepte le numéro de téléphone « %s »',
      (numero) => {
        saisir('telephone', numero);

        expect(composant.form.controls.telephone.valid).toBe(true);
      },
    );

    it.each(['06.12.34.56.78', '06-abc', '(06) 12 34'])(
      'refuse le numéro de téléphone « %s »',
      (numero) => {
        saisir('telephone', numero);

        expect(composant.messageErreur('telephone')).toBe('Invalid phone number.');
      },
    );

    it("refuse une date de naissance égale à aujourd'hui", () => {
      saisir('dateNaissance', aujourdhui());

      expect(composant.messageErreur('dateNaissance')).toBe('Date of birth must be in the past.');
    });

    it('accepte une date de naissance passée', () => {
      saisir('dateNaissance', '2001-06-30');

      expect(composant.form.controls.dateNaissance.valid).toBe(true);
    });

    it("affiche en priorité l'erreur du serveur sur un champ", () => {
      const controle = composant.form.controls.email;
      controle.setErrors({ required: true, serveur: 'Adresse déjà utilisée.' });
      controle.markAsTouched();

      expect(composant.messageErreur('email')).toBe('Adresse déjà utilisée.');
    });

    it('envoie null pour les champs facultatifs vides', () => {
      saisir('telephone', '   ');
      saisir('adresse', '');
      saisir('dateNaissance', '');
      composant.enregistrer();

      const requete = TestBed.inject(HttpTestingController).expectOne('/api/etudiants');
      expect(requete.request.body).toEqual(
        expect.objectContaining({ telephone: null, adresse: null, dateNaissance: null }),
      );
      requete.flush({ id: 1, ...requete.request.body });
    });
  });
});
