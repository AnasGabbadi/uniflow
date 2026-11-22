import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MESSAGE_SERVEUR_INJOIGNABLE } from '../../services/api-error-messages';
import { Students } from './students';

describe('Students', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Students],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  function render() {
    const fixture = TestBed.createComponent(Students);
    fixture.detectChanges();

    // Simuler la réponse de la requête des formations
    http.expectOne((req) => req.url.includes('/api/formations')).flush([]);

    return fixture;
  }

  it('affiche les étudiants', async () => {
    const fixture = render();

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush([
        {
          id: 1,
          nom: 'El Amrani',
          prenom: 'Sara',
          email: 's@x.ma',
          telephone: null,
          adresse: null,
          dateNaissance: null,
        },
      ]);

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El Amrani');
  });

  it('affiche un message si la liste est vide', async () => {
    const fixture = render();

    http.expectOne((req) => req.url.includes('/api/etudiants')).flush([]);

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun étudiant');
  });

  it('affiche une erreur si le chargement échoue', async () => {
    const fixture = render();

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush(null, {
        status: 500,
        statusText: 'Server Error',
      });

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });

  it('affiche un message anglais lisible et Retry quand Nginx renvoie 502', async () => {
    const fixture = render();

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush('<html>502 Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' });
    await fixture.whenStable();
    fixture.detectChanges();

    const alerte = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerte?.textContent).toContain(MESSAGE_SERVEUR_INJOIGNABLE);

    (alerte?.querySelector('button') as HTMLButtonElement).click();
    http.expectOne((req) => req.url.includes('/api/etudiants')).flush([]);
    http.expectOne((req) => req.url.includes('/api/formations')).flush([]);
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBeNull();
  });

  it("n'affiche ni le tableau ni l'état vide si le chargement échoue", async () => {
    const fixture = render();

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('app-ui-alert button')?.textContent).toContain('Retry');
    expect(element.querySelector('table')).toBeNull();
    expect(element.querySelector('.empty-state')).toBeNull();
    expect(element.textContent).not.toContain('Aucun étudiant');
  });

  it("n'affiche pas l'état vide si le filtre par formation échoue", async () => {
    const fixture = render();
    http.expectOne((req) => req.url.includes('/api/etudiants')).flush([]);
    await fixture.whenStable();

    fixture.componentInstance.changerFormation('3');
    http
      .expectOne((req) => req.url.includes('/api/formations/3'))
      .error(new ProgressEvent('error'));
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    expect(element.querySelector('.empty-state')).toBeNull();
  });
});
