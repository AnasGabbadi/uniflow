import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  /**
   * Barre de progression globale : visible tant qu'au moins une requête HTTP est en cours.
   * Elle est décorative (aria-hidden) : chaque page annonce déjà son chargement via app-ui-loading.
   */
  readonly chargement = inject(LoadingService).actif;
}
