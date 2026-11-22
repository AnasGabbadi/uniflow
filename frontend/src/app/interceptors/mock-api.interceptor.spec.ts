import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { mockApiInterceptor } from './mock-api.interceptor';

/**
 * Seul le passage direct (mode mock désactivé) est testé. Pour ne pas dépendre de la valeur
 * de `useMock` dans les fichiers d'environnement, le test la force à false puis la restaure.
 */
describe('mockApiInterceptor (mode mock désactivé)', () => {
  let http: HttpClient;
  let controleur: HttpTestingController;
  let valeurInitiale: boolean;

  beforeEach(() => {
    valeurInitiale = environment.useMock;
    environment.useMock = false;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([mockApiInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controleur = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controleur.verify();
    environment.useMock = valeurInitiale;
  });

  it("transmet les requêtes de l'API au back-end, sans réponse simulée", () => {
    let resultat: unknown;
    http.get('/api/formations').subscribe((r) => (resultat = r));

    const requete = controleur.expectOne('/api/formations');
    requete.flush([{ id: 42 }]);

    expect(resultat).toEqual([{ id: 42 }]);
  });

  it("transmet aussi les requêtes hors de l'API", () => {
    let resultat: unknown;
    http.get('/assets/config.json').subscribe((r) => (resultat = r));

    controleur.expectOne('/assets/config.json').flush({ theme: 'light' });

    expect(resultat).toEqual({ theme: 'light' });
  });
});
