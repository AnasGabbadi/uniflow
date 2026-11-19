import { Routes } from '@angular/router';

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

  {
    path: 'students',
    component: Students,
  },

  {
    path: 'students/:id',
    component: StudentDetail,
  },

  {
    path: 'trainings',
    component: Trainings,
  },

  {
    path: 'trainings/new',
    component: TrainingForm,
  },

  {
    path: 'trainings/:id',
    component: TrainingDetail,
  },

  {
    path: 'trainings/:id/edit',
    component: TrainingForm,
  },

  {
    path: 'enrollments',
    component: Enrollments,
  },

  {
    path: '**',
    redirectTo: 'students',
  },
];
