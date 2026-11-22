import { Routes } from '@angular/router';
import { StudentForm } from './pages/student-form/student-form';
import { Students } from './pages/students/students';
import { StudentDetail } from './pages/student-detail/student-detail';
import { Trainings } from './pages/trainings/trainings';
import { TrainingDetail } from './pages/trainings/training-detail/training-detail';
import { TrainingForm } from './pages/trainings/training-form/training-form';
import { Enrollments } from './pages/enrollments/enrollments';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'students',
    pathMatch: 'full',
  },

  // --- Students : du plus spécifique au plus générique ---
  {
    path: 'students',
    component: Students,
  },
  {
    path: 'students/new',
    component: StudentForm,
  },
  {
    path: 'students/:id/edit',
    component: StudentForm,
  },
  {
    path: 'students/:id',
    component: StudentDetail,
  },

  // --- Trainings : même règle ---
  {
    path: 'trainings',
    component: Trainings,
  },
  {
    path: 'trainings/new',
    component: TrainingForm,
  },
  {
    path: 'trainings/:id/edit',
    component: TrainingForm,
  },
  {
    path: 'trainings/:id',
    component: TrainingDetail,
  },

  // --- Enrollments ---
  {
    path: 'enrollments',
    component: Enrollments,
  },

  // --- Wildcard en dernier ---
  {
    path: '**',
    redirectTo: 'students',
  },
];
