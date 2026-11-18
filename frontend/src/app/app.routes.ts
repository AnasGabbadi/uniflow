import { Routes } from '@angular/router';

import { Students } from './pages/students/students';
import { StudentDetail } from './pages/student-detail/student-detail';
import { Trainings } from './pages/trainings/trainings';
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
    path: 'enrollments',
    component: Enrollments,
  },

  {
    path: '**',
    redirectTo: 'students',
  },
];
