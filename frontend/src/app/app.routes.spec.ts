import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';
import { Enrollments } from './pages/enrollments/enrollments';
import { StudentDetail } from './pages/student-detail/student-detail';
import { StudentForm } from './pages/student-form/student-form';
import { Students } from './pages/students/students';
import { TrainingDetail } from './pages/trainings/training-detail/training-detail';
import { TrainingForm } from './pages/trainings/training-form/training-form';
import { Trainings } from './pages/trainings/trainings';

describe('routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it.each([
    ['/students', Students],
    ['/students/new', StudentForm],
    ['/students/4/edit', StudentForm],
    ['/students/4', StudentDetail],
    ['/trainings', Trainings],
    ['/trainings/new', TrainingForm],
    ['/trainings/4/edit', TrainingForm],
    ['/trainings/4', TrainingDetail],
    ['/enrollments', Enrollments],
  ])('affiche le bon composant pour %s', async (url, composant) => {
    const harness = await RouterTestingHarness.create();

    const instance = await harness.navigateByUrl(url);

    expect(instance).toBeInstanceOf(composant);
  });

  it.each(['/', '/page-inconnue'])('redirige %s vers /students', async (url) => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl(url);

    expect(TestBed.inject(Router).url).toBe('/students');
  });
});
