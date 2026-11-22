import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Inscription } from '../../models/inscription.model';
import { MESSAGES_PAR_CODE, MESSAGE_SERVEUR_INJOIGNABLE } from '../../services/api-error-messages';
import { Enrollments } from './enrollments';

describe('Enrollments', () => {
  let component: Enrollments;
  let fixture: ComponentFixture<Enrollments>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Enrollments],
    }).compileComponents();

    fixture = TestBed.createComponent(Enrollments);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('Enrollments — chargement et erreurs (IP-45)', () => {
  let http: HttpTestingController;

  const inscription = {
    id: 4,
    etudiant: { id: 1, nom: 'El Amrani', prenom: 'Sara', email: 'sara@example.ma' },
    formation: { id: 2, intitule: 'DevOps' },
    dateInscription: '2026-11-20',
    statut: 'EN_ATTENTE',
  } as Inscription;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Enrollments],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function charger(inscriptions: Inscription[]): ComponentFixture<Enrollments> {
    const fixture = TestBed.createComponent(Enrollments);
    http.expectOne('/api/inscriptions').flush(inscriptions);
    http.expectOne('/api/etudiants').flush([]);
    http.expectOne('/api/formations').flush([]);
    fixture.detectChanges();
    return fixture;
  }

  it("l'échec d'une liste secondaire n'écrase pas l'erreur principale", () => {
    const fixture = TestBed.createComponent(Enrollments);
    http.expectOne('/api/inscriptions').error(new ProgressEvent('error'));
    http
      .expectOne('/api/etudiants')
      .flush({ code: 'ERREUR_INTERNE' }, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/formations').flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.erreur()).toBe(MESSAGE_SERVEUR_INJOIGNABLE);
    expect(fixture.componentInstance.chargement()).toBe(false);
  });

  it('traduit le 409 INSCRIPTION_EN_DOUBLE en anglais', () => {
    const fixture = charger([]);
    const composant = fixture.componentInstance;
    composant.nouvelleInscription.etudiantId = 1;
    composant.nouvelleInscription.formationId = 2;

    composant.creer();
    http
      .expectOne('/api/inscriptions')
      .flush(
        { detail: "L'étudiant est déjà inscrit.", code: 'INSCRIPTION_EN_DOUBLE' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(composant.erreur()).toBe(MESSAGES_PAR_CODE.INSCRIPTION_EN_DOUBLE);
    expect(composant.saving()).toBe(false);
  });

  it('désactive « Cancel enrollment » pendant la requête d’annulation', () => {
    const fixture = charger([inscription]);
    const composant = fixture.componentInstance;

    composant.demanderAnnulation(inscription);
    composant.confirmerAnnulation();
    fixture.detectChanges();

    const bouton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.link-danger',
    ) as HTMLButtonElement;
    expect(composant.annulationEnCours()).toBe(true);
    expect(bouton.disabled).toBe(true);

    http
      .expectOne((req) => req.url.startsWith('/api/inscriptions/4'))
      .flush({ ...inscription, statut: 'ANNULEE' });
    fixture.detectChanges();

    expect(composant.annulationEnCours()).toBe(false);
    expect(composant.succes()).toContain('cancelled');
  });

  it("n'affiche ni le tableau ni l'état vide si le chargement échoue", () => {
    const fixture = TestBed.createComponent(Enrollments);
    http
      .expectOne('/api/inscriptions')
      .flush('<html>502 Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' });
    http.expectOne('/api/etudiants').flush([]);
    http.expectOne('/api/formations').flush([]);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.chargementEchoue()).toBe(true);
    expect(element.querySelector('app-ui-alert button')?.textContent).toContain('Retry');
    expect(element.querySelector('table')).toBeNull();
    expect(element.textContent).not.toContain('No enrollments found');
  });

  it("affiche l'état vide seulement si le chargement réussit avec une liste vide", () => {
    const fixture = charger([]);
    const element = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.chargementEchoue()).toBe(false);
    expect(element.textContent).toContain('No enrollments found');
  });

  it("n'appelle pas l'API si l'étudiant ou la formation n'est pas sélectionné", () => {
    const composant = charger([]).componentInstance;
    composant.afficherFormulaire();
    composant.nouvelleInscription.etudiantId = 1;

    composant.creer();

    http.expectNone('/api/inscriptions');
    expect(composant.erreur()).toBe('Please select a student and a training.');
  });

  it('crée une inscription au format du contrat { etudiantId, formationId }', () => {
    const fixture = charger([]);
    const composant = fixture.componentInstance;
    composant.afficherFormulaire();
    composant.nouvelleInscription.etudiantId = 1;
    composant.nouvelleInscription.formationId = 2;

    composant.creer();
    expect(composant.saving()).toBe(true);

    const requete = http.expectOne('/api/inscriptions');
    expect(requete.request.method).toBe('POST');
    // Contrat d'API §5 (le back-end attend aujourd'hui idEtudiant/idFormation : bug connu).
    expect(requete.request.body).toEqual({ etudiantId: 1, formationId: 2 });
    requete.flush(inscription, { status: 201, statusText: 'Created' });

    expect(composant.saving()).toBe(false);
    expect(composant.succes()).toBe('Enrollment created successfully!');
    expect(composant.formulaireVisible()).toBe(false);
    expect(composant.inscriptions()).toEqual([inscription]);
    expect(composant.nouvelleInscription).toEqual({ etudiantId: 0, formationId: 0 });
  });

  it('réinitialise la sélection et les messages à chaque ouverture du formulaire', () => {
    const composant = charger([]).componentInstance;
    composant.nouvelleInscription.etudiantId = 5;
    composant.erreur.set('Old error');
    composant.succes.set('Old success');

    composant.afficherFormulaire();

    expect(composant.formulaireVisible()).toBe(true);
    expect(composant.nouvelleInscription).toEqual({ etudiantId: 0, formationId: 0 });
    expect(composant.erreur()).toBeNull();
    expect(composant.succes()).toBeNull();

    composant.annulerFormulaire();
    expect(composant.formulaireVisible()).toBe(false);
  });
});
