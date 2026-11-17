
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-form-field',
  standalone: true,
  templateUrl: './ui-form-field.html',
  styleUrl: './ui-form-field.scss'
})
export class UiFormField {
  @Input() label = '';
  @Input() required = false;
}
