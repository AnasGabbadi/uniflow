import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Students } from './students';

describe('Students', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Students],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify(); // aucune requête ne doit rester en vol
  });

  function render() {
    const fixture = TestBed.createComponent(Students);
    fixture.detectChanges(); // déclenche ngOnInit → load() → requête HTTP
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

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush([]);

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun étudiant');
  });

  it('affiche une erreur si le chargement échoue', async () => {
    const fixture = render();

    http
      .expectOne((req) => req.url.includes('/api/etudiants'))
      .flush(null, { status: 500, statusText: 'Server Error' });

    await fixture.whenStable();
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[role="alert"]'),
    ).toBeTruthy();
  });
});
