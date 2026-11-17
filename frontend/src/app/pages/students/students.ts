import { Component } from '@angular/core';
import { UiTable } from '../../components/shared/ui-table/ui-table';
import { UiFormField } from '../../components/shared/ui-form-field/ui-form-field';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [UiTable, UiFormField],
  templateUrl: './students.html'
})
export class Students {
  columns = ['Name', 'Email', 'Phone'];

  students = [
    {
      Name: 'Ahmed',
      Email: 'ahmed@gmail.com',
      Phone: '0612345678'
    },
    {
      Name: 'Sara',
      Email: 'sara@gmail.com',
      Phone: '0623456789'
    }
  ];
}
