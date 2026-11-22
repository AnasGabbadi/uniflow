import { Component, input, output } from '@angular/core';

import { UiButton } from '../ui-button/ui-button';

/**
 * Message d'erreur ou de succès commun à toutes les pages.
 * - variant "error" : role="alert" (annoncé immédiatement par les lecteurs d'écran) ;
 * - variant "success" : role="status".
 * Avec `retryable`, un bouton « Retry » émet `retry` (utile après une panne réseau).
 */
@Component({
  selector: 'app-ui-alert',
  imports: [UiButton],
  templateUrl: './ui-alert.html',
  styleUrl: './ui-alert.scss',
})
export class UiAlert {
  readonly message = input.required<string>();
  readonly variant = input<'error' | 'success'>('error');
  readonly retryable = input(false);

  readonly retry = output<void>();
}
