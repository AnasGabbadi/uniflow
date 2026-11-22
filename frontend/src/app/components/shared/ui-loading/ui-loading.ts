import { Component, input } from '@angular/core';

/** Indicateur de chargement commun : spinner + libellé, annoncé poliment aux lecteurs d'écran. */
@Component({
  selector: 'app-ui-loading',
  templateUrl: './ui-loading.html',
  styleUrl: './ui-loading.scss',
})
export class UiLoading {
  readonly label = input('Loading…');
}
