import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Formation } from '../../models/formation.model';
import { Trainings } from './trainings';

function formation(id: number, intitule: string): Formation {
  return {
    id,
    intitule,
    description: null,
    duree: 10,
    capaciteMax: 20,
    dateDebut: '2026-12-01',
    dateFin: '2026-12-05',
    statut: 'OUVERTE',
    placesRestantes: 20,
  };
}

describe('Trainings', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Trainings],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("affiche les formations renvoyées par l'API", async () => {
    const fixture = TestBed.createComponent(Trainings);
    http.expectOne('/api/formations').flush([formation(1, 'DevOps'), formation(2, 'Spring')]);
    fixture.detectChanges();
    await fixture.whenStable();

    const lignes = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('DevOps');
    expect(fixture.componentInstance.chargement()).toBe(false);
  });

  it("affiche le message d'erreur de l'API", () => {
    const fixture = TestBed.createComponent(Trainings);
    http
      .expectOne('/api/formations')
      .flush(
        { detail: 'Service indisponible' },
        { status: 503, statusText: 'Service Unavailable' },
      );

    expect(fixture.componentInstance.erreur()).toBe('Service indisponible');
    expect(fixture.componentInstance.chargement()).toBe(false);
  });

  it('supprime une formation après confirmation', () => {
    const fixture = TestBed.createComponent(Trainings);
    const composant = fixture.componentInstance;
    http.expectOne('/api/formations').flush([formation(1, 'DevOps'), formation(2, 'Spring')]);

    composant.demanderSuppression(composant.formations()[0]);
    expect(composant.messageSuppression()).toContain('DevOps');
    composant.confirmerSuppression();

    const requete = http.expectOne('/api/formations/1');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null, { status: 204, statusText: 'No Content' });

    expect(composant.formations().map((f) => f.id)).toEqual([2]);
    expect(composant.aSupprimer()).toBeNull();
  });

  it("n'appelle pas l'API si la suppression est annulée", () => {
    const fixture = TestBed.createComponent(Trainings);
    const composant = fixture.componentInstance;
    http.expectOne('/api/formations').flush([formation(1, 'DevOps')]);

    composant.demanderSuppression(composant.formations()[0]);
    composant.annulerSuppression();
    composant.confirmerSuppression();

    http.expectNone('/api/formations/1');
    expect(composant.formations().length).toBe(1);
  });
});
