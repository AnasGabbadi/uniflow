import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Inscription } from '../models/inscription.model';
import { InscriptionService } from './inscription.service';

describe('InscriptionService', () => {
  let service: InscriptionService;
  let http: HttpTestingController;

  const inscription: Inscription = {
    id: 4,
    dateInscription: '2026-11-20',
    statut: 'EN_ATTENTE',
    etudiant: { id: 1, nom: 'El Amrani', prenom: 'Sara', email: 'sara@example.ma' },
    formation: { id: 3, intitule: 'DevOps' },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InscriptionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste toutes les inscriptions sans paramètre', () => {
    let resultat: Inscription[] | undefined;
    service.lister().subscribe((liste) => (resultat = liste));

    const appel = http.expectOne((req) => req.url === '/api/inscriptions');
    expect(appel.request.method).toBe('GET');
    expect(appel.request.params.keys()).toEqual([]);
    appel.flush([inscription]);

    expect(resultat).toEqual([inscription]);
  });

  it('envoie les trois filtres renseignés en paramètres', () => {
    service.lister({ etudiantId: 1, formationId: 3, statut: 'VALIDEE' }).subscribe();

    const appel = http.expectOne((req) => req.url === '/api/inscriptions');
    expect(appel.request.params.get('etudiantId')).toBe('1');
    expect(appel.request.params.get('formationId')).toBe('3');
    expect(appel.request.params.get('statut')).toBe('VALIDEE');
    appel.flush([]);
  });

  it("n'envoie que les filtres renseignés", () => {
    service.lister({ formationId: 3 }).subscribe();

    const appel = http.expectOne((req) => req.url === '/api/inscriptions');
    expect(appel.request.params.keys()).toEqual(['formationId']);
    expect(appel.request.urlWithParams).toBe('/api/inscriptions?formationId=3');
    appel.flush([]);
  });

  it('lit une inscription avec GET /api/inscriptions/{id}', () => {
    let resultat: Inscription | undefined;
    service.obtenir(4).subscribe((i) => (resultat = i));

    const appel = http.expectOne('/api/inscriptions/4');
    expect(appel.request.method).toBe('GET');
    appel.flush(inscription);

    expect(resultat?.formation.intitule).toBe('DevOps');
  });

  it('crée une inscription avec POST au format du contrat { etudiantId, formationId }', () => {
    // docs/api-contract.md §5 : { "etudiantId": 1, "formationId": 3 }.
    // (Le back-end attend aujourd'hui idEtudiant/idFormation : bug connu, non validé ici.)
    let resultat: Inscription | undefined;
    service.creer({ etudiantId: 1, formationId: 3 }).subscribe((i) => (resultat = i));

    const appel = http.expectOne('/api/inscriptions');
    expect(appel.request.method).toBe('POST');
    expect(appel.request.body).toEqual({ etudiantId: 1, formationId: 3 });
    appel.flush(inscription, { status: 201, statusText: 'Created' });

    expect(resultat?.statut).toBe('EN_ATTENTE');
  });

  it('change le statut avec PATCH /api/inscriptions/{id}/statut et le corps { statut }', () => {
    let resultat: Inscription | undefined;
    service.changerStatut(4, 'ANNULEE').subscribe((i) => (resultat = i));

    const appel = http.expectOne('/api/inscriptions/4/statut');
    expect(appel.request.method).toBe('PATCH');
    expect(appel.request.body).toEqual({ statut: 'ANNULEE' });
    appel.flush({ ...inscription, statut: 'ANNULEE' });

    expect(resultat?.statut).toBe('ANNULEE');
  });
});
