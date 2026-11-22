import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { MESSAGE_SERVEUR_INJOIGNABLE } from '../../services/api-error-messages';
import { StudentDetail } from './student-detail';

describe('StudentDetail', () => {
  let http: HttpTestingController;

  function configurer(id: string): void {
    TestBed.configureTestingModule({
      imports: [StudentDetail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id }) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  }

  afterEach(() => http.verify());

  it("affiche l'indicateur de chargement puis l'étudiant", () => {
    configurer('7');
    const fixture = TestBed.createComponent(StudentDetail);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('app-ui-loading')).not.toBeNull();

    http.expectOne('/api/etudiants/7').flush({
      id: 7,
      nom: 'El Amrani',
      prenom: 'Sara',
      email: 'sara@example.ma',
      telephone: null,
      adresse: null,
      dateNaissance: null,
    });
    fixture.detectChanges();

    expect(element.querySelector('app-ui-loading')).toBeNull();
    expect(element.querySelector('h1')?.textContent).toContain('Sara El Amrani');
  });

  it('affiche « Student not found » pour un 404', () => {
    configurer('7');
    const fixture = TestBed.createComponent(StudentDetail);
    fixture.detectChanges();

    http
      .expectOne('/api/etudiants/7')
      .flush({ code: 'RESSOURCE_INTROUVABLE' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(fixture.componentInstance.notFound()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Student not found');
  });

  it('affiche « Student not found » sans appeler l’API pour un identifiant invalide', () => {
    configurer('abc');
    const fixture = TestBed.createComponent(StudentDetail);
    fixture.detectChanges();

    expect(fixture.componentInstance.notFound()).toBe(true);
  });

  it('gère une panne réseau sans casser la page, puis réessaie', () => {
    configurer('7');
    const fixture = TestBed.createComponent(StudentDetail);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    http.expectOne('/api/etudiants/7').error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      MESSAGE_SERVEUR_INJOIGNABLE,
    );
    expect(element.querySelector('a')?.textContent).toContain('Back to students');

    (element.querySelector('app-ui-alert button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBeNull();
    expect(fixture.componentInstance.loading()).toBe(true);
    http.expectOne('/api/etudiants/7').flush({
      id: 7,
      nom: 'El Amrani',
      prenom: 'Sara',
      email: 'sara@example.ma',
      telephone: null,
      adresse: null,
      dateNaissance: null,
    });
    expect(fixture.componentInstance.student()?.nom).toBe('El Amrani');
  });
});
