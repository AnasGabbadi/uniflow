import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-form-field',
  templateUrl: './ui-form-field.html',
  styleUrl: './ui-form-field.scss',
})
export class UiFormField {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) controlId!: string;
  @Input() required = false;
}
