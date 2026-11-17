
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  templateUrl: './ui-button.html',
  styleUrl: './ui-button.scss'
})
export class UiButton {
  @Input() label = 'Submit';
  @Input() variant = 'primary';
  @Input() disabled = false;

  @Output() clicked = new EventEmitter<void>();
}
