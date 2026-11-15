import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Etudiant } from '../models/etudiant.model';
import { EtudiantService } from './etudiant.service';

describe('EtudiantService', () => {
  let service: EtudiantService;
  let http: HttpTestingController;

  const sara: Etudiant = {
    id: 1,
    nom: 'El Amrani',
    prenom: 'Sara',
    email: 'sara.elamrani@example.ma',
    telephone: null,
    adresse: null,
    dateNaissance: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EtudiantService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envoie le terme de recherche nettoyé', () => {
    let resultat: Etudiant[] = [];
    service.lister('  sara ').subscribe((liste) => (resultat = liste));

    const req = http.expectOne((r) => r.url === '/api/etudiants');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('recherche')).toBe('sara');
    req.flush([sara]);

    expect(resultat).toEqual([sara]);
  });

  it("n'envoie pas de paramètre pour une recherche vide", () => {
    service.lister('   ').subscribe();

    const req = http.expectOne('/api/etudiants');
    expect(req.request.params.has('recherche')).toBe(false);
    req.flush([]);
  });

  it('crée un étudiant avec POST', () => {
    const { id, ...requete } = sara;
    service.creer(requete).subscribe();

    const req = http.expectOne('/api/etudiants');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(requete);
    req.flush({ ...requete, id });
  });

  it('supprime un étudiant avec DELETE', () => {
    service.supprimer(1).subscribe();

    const req = http.expectOne('/api/etudiants/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
