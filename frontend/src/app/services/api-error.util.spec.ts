import { HttpErrorResponse } from '@angular/common/http';

import { lireErreurApi } from './api-error.util';

describe('lireErreurApi', () => {
  it('lit le format Problem Details (IP-22)', () => {
    const erreur = new HttpErrorResponse({
      status: 400,
      error: {
        title: 'Données invalides',
        status: 400,
        detail: 'Un ou plusieurs champs sont invalides.',
        code: 'VALIDATION',
        erreurs: [{ champ: 'intitule', message: 'obligatoire' }],
      },
    });

    const resultat = lireErreurApi(erreur);

    expect(resultat.message).toBe('Un ou plusieurs champs sont invalides.');
    expect(resultat.code).toBe('VALIDATION');
    expect(resultat.champs).toEqual({ intitule: 'obligatoire' });
  });

  it("lit l'ancien format (message + erreurs sous forme d'objet)", () => {
    const erreur = new HttpErrorResponse({
      status: 409,
      error: { status: 409, message: 'Intitulé déjà utilisé', erreurs: { duree: 'invalide' } },
    });

    const resultat = lireErreurApi(erreur);

    expect(resultat.message).toBe('Intitulé déjà utilisé');
    expect(resultat.code).toBeUndefined();
    expect(resultat.champs).toEqual({ duree: 'invalide' });
  });

  it('signale un serveur injoignable', () => {
    const resultat = lireErreurApi(new HttpErrorResponse({ status: 0 }));

    expect(resultat.message).toContain('Unable to reach the server');
  });

  it("renvoie un message générique pour une erreur qui n'est pas HTTP", () => {
    expect(lireErreurApi(new Error('boom')).message).toContain('unexpected error');
  });
});
