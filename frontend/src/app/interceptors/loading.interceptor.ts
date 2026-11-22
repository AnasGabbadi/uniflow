import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';

import { LoadingService } from '../services/loading.service';

/**
 * Signale chaque requête HTTP au LoadingService, du départ jusqu'à la fin
 * (succès, erreur ou annulation), pour afficher l'indicateur de chargement global.
 * À déclarer AVANT mockApiInterceptor pour couvrir aussi les réponses simulées.
 */
export const loadingInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const chargement = inject(LoadingService);
  chargement.demarrer();
  return suivant(requete).pipe(finalize(() => chargement.terminer()));
};
