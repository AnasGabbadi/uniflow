import { HttpErrorResponse } from '@angular/common/http';

import { CodeErreur } from '../models/api-error.model';
import {
  MESSAGE_INATTENDU,
  MESSAGE_SERVEUR_INJOIGNABLE,
  MESSAGES_PAR_CODE,
  estServeurInjoignable,
  messageErreurApi,
} from './api-error-messages';

function erreurHttp(status: number, error: unknown = null): HttpErrorResponse {
  return new HttpErrorResponse({ status, error });
}

describe('messageErreurApi', () => {
  it('traduit chaque code de l’API en anglais, même si le detail est en français', () => {
    const erreur = erreurHttp(409, {
      status: 409,
      detail: "L'étudiant est déjà inscrit à cette formation.",
      code: 'INSCRIPTION_EN_DOUBLE',
    });

    expect(messageErreurApi(erreur)).toBe('This student is already enrolled in this training.');
  });

  it('définit un message pour tous les codes de CodeErreur', () => {
    const codes: CodeErreur[] = [
      'VALIDATION',
      'REQUETE_INVALIDE',
      'RESSOURCE_INTROUVABLE',
      'EMAIL_DEJA_UTILISE',
      'INTITULE_DEJA_UTILISE',
      'CAPACITE_INSUFFISANTE',
      'INSCRIPTION_EN_DOUBLE',
      'FORMATION_COMPLETE',
      'FORMATION_NON_OUVERTE',
      'TRANSITION_INTERDITE',
      'ERREUR_INTERNE',
    ];
    for (const code of codes) {
      expect(MESSAGES_PAR_CODE[code]).toMatch(/\S/);
      expect(messageErreurApi(erreurHttp(400, { code }))).toBe(MESSAGES_PAR_CODE[code]);
    }
  });

  it('se replie sur le detail de l’API quand le code est absent ou inconnu', () => {
    expect(messageErreurApi(erreurHttp(409, { detail: 'Detail from API' }))).toBe(
      'Detail from API',
    );
    expect(messageErreurApi(erreurHttp(409, { detail: 'Autre', code: 'CODE_INCONNU' }))).toBe(
      'Autre',
    );
  });

  it("se replie sur le message de l'ancien format", () => {
    const erreur = erreurHttp(409, { message: "Une formation avec l'intitulé existe déjà" });

    expect(messageErreurApi(erreur)).toContain('existe déjà');
  });

  it.each([
    [400, 'The request is invalid. Please check the entered values.'],
    [404, 'The requested item was not found. It may have been deleted.'],
    [409, 'This action conflicts with existing data.'],
    [500, 'An internal server error occurred. Please try again later.'],
    [501, 'An internal server error occurred. Please try again later.'],
  ])('donne un message selon le statut %i quand le corps est vide', (status, attendu) => {
    expect(messageErreurApi(erreurHttp(status))).toBe(attendu);
  });

  it.each([0, 502, 503, 504])('traite le statut %i comme un serveur injoignable', (status) => {
    // Nginx renvoie 502 avec une page HTML quand l'API est arrêtée.
    const erreur = erreurHttp(status, '<html>Bad Gateway</html>');

    expect(messageErreurApi(erreur)).toBe(MESSAGE_SERVEUR_INJOIGNABLE);
    expect(estServeurInjoignable(erreur)).toBe(true);
  });

  it('applique les surcharges de la page par code, puis par statut', () => {
    const introuvable = erreurHttp(404, { code: 'RESSOURCE_INTROUVABLE' });

    expect(messageErreurApi(introuvable, { RESSOURCE_INTROUVABLE: 'Training not found.' })).toBe(
      'Training not found.',
    );
    expect(messageErreurApi(introuvable, { 404: 'Gone.' })).toBe('Gone.');
    expect(messageErreurApi(erreurHttp(0), { 0: 'Offline.' })).toBe('Offline.');
  });

  it('renvoie le message générique pour une erreur non HTTP ou un statut inattendu', () => {
    expect(messageErreurApi(new Error('boom'))).toBe(MESSAGE_INATTENDU);
    expect(messageErreurApi(undefined)).toBe(MESSAGE_INATTENDU);
    expect(messageErreurApi(erreurHttp(418))).toBe(MESSAGE_INATTENDU);
    expect(estServeurInjoignable(new Error('boom'))).toBe(false);
    expect(estServeurInjoignable(erreurHttp(500))).toBe(false);
  });
});
