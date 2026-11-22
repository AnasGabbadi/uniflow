import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Formation } from '../../models/formation.model';
import { MESSAGES_PAR_CODE, MESSAGE_SERVEUR_INJOIGNABLE } from '../../services/api-error-messages';
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
        { detail: 'Données invalides', code: 'REQUETE_INVALIDE' },
        { status: 400, statusText: 'Bad Request' },
      );

    // IP-45 : le code de l'API est traduit en anglais.
    expect(fixture.componentInstance.erreur()).toBe(MESSAGES_PAR_CODE.REQUETE_INVALIDE);
    expect(fixture.componentInstance.chargement()).toBe(false);
  });

  it('traite un 503 (API arrêtée derrière Nginx) comme un serveur injoignable, avec Retry', () => {
    const fixture = TestBed.createComponent(Trainings);
    http
      .expectOne('/api/formations')
      .flush(
        { detail: 'Service indisponible' },
        { status: 503, statusText: 'Service Unavailable' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.erreur()).toBe(MESSAGE_SERVEUR_INJOIGNABLE);
    const bouton = (fixture.nativeElement as HTMLElement).querySelector(
      'app-ui-alert button',
    ) as HTMLButtonElement;
    bouton.click();

    http.expectOne('/api/formations').flush([formation(1, 'DevOps')]);
    expect(fixture.componentInstance.erreur()).toBeNull();
    expect(fixture.componentInstance.formations().length).toBe(1);
  });

  it('désactive les boutons « Delete » pendant la suppression (pas de double clic)', () => {
    const fixture = TestBed.createComponent(Trainings);
    const composant = fixture.componentInstance;
    http.expectOne('/api/formations').flush([formation(1, 'DevOps'), formation(2, 'Spring')]);

    composant.demanderSuppression(composant.formations()[0]);
    composant.confirmerSuppression();
    fixture.detectChanges();

    const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'button.link-danger',
    );
    expect(composant.suppressionEnCours()).toBe(true);
    expect([...boutons].every((b) => b.disabled)).toBe(true);

    http
      .expectOne('/api/formations/1')
      .flush({ code: 'RESSOURCE_INTROUVABLE' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(composant.suppressionEnCours()).toBe(false);
    expect(composant.erreur()).toBe(MESSAGES_PAR_CODE.RESSOURCE_INTROUVABLE);
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

  it("n'affiche ni le tableau ni l'état vide si le chargement échoue", () => {
    const fixture = TestBed.createComponent(Trainings);
    http.expectOne('/api/formations').error(new ProgressEvent('error'));
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      MESSAGE_SERVEUR_INJOIGNABLE,
    );
    expect(element.querySelector('app-ui-alert button')?.textContent).toContain('Retry');
    expect(element.querySelector('table')).toBeNull();
    expect(element.textContent).not.toContain('No training yet');
  });

  it("affiche l'état vide seulement si le chargement réussit avec une liste vide", () => {
    const fixture = TestBed.createComponent(Trainings);
    http.expectOne('/api/formations').flush([]);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(element.textContent).toContain('No training yet');
  });
});
