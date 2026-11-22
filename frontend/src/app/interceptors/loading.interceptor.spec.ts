import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LoadingService } from '../services/loading.service';
import { loadingInterceptor } from './loading.interceptor';

describe('loadingInterceptor', () => {
  let http: HttpClient;
  let controleur: HttpTestingController;
  let chargement: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loadingInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controleur = TestBed.inject(HttpTestingController);
    chargement = TestBed.inject(LoadingService);
  });

  afterEach(() => controleur.verify());

  it('active le chargement pendant la requête et le coupe après le succès', () => {
    http.get('/api/formations').subscribe();
    expect(chargement.actif()).toBe(true);

    controleur.expectOne('/api/formations').flush([]);
    expect(chargement.actif()).toBe(false);
  });

  it('coupe le chargement après une erreur HTTP', () => {
    http.get('/api/formations/9').subscribe({ error: () => undefined });

    controleur.expectOne('/api/formations/9').flush(null, { status: 404, statusText: 'Not Found' });
    expect(chargement.actif()).toBe(false);
  });

  it('coupe le chargement après une panne réseau', () => {
    http.get('/api/etudiants').subscribe({ error: () => undefined });

    controleur.expectOne('/api/etudiants').error(new ProgressEvent('error'));
    expect(chargement.actif()).toBe(false);
  });

  it('compte les requêtes parallèles et coupe le chargement après la dernière', () => {
    http.get('/api/etudiants').subscribe();
    http.get('/api/formations').subscribe();
    expect(chargement.requetesEnCours()).toBe(2);

    controleur.expectOne('/api/etudiants').flush([]);
    expect(chargement.actif()).toBe(true);

    controleur.expectOne('/api/formations').flush([]);
    expect(chargement.actif()).toBe(false);
  });

  it('coupe le chargement si la requête est annulée (désabonnement)', () => {
    const abonnement = http.get('/api/inscriptions').subscribe();
    expect(chargement.actif()).toBe(true);

    abonnement.unsubscribe();
    expect(chargement.actif()).toBe(false);
    controleur.match('/api/inscriptions');
  });
});
