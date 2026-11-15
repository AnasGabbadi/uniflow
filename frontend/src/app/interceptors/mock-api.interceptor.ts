import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { delay, of, throwError } from 'rxjs';

import { environment } from '../../environments/environment';
import { Etudiant } from '../models/etudiant.model';
import { Formation } from '../models/formation.model';
import { Inscription } from '../models/inscription.model';

/**
 * Simulateur d'API pour développer le front avant que le back-end soit prêt.
 * Actif uniquement si `environment.useMock` vaut true.
 * Les lectures renvoient des données d'exemple ; les écritures sont acceptées sans être enregistrées.
 */
const ETUDIANTS: Etudiant[] = [
  {
    id: 1,
    nom: 'El Amrani',
    prenom: 'Sara',
    email: 'sara.elamrani@example.ma',
    telephone: '+212 6 12 34 56 78',
    adresse: 'Casablanca',
    dateNaissance: '2003-04-21',
  },
  {
    id: 2,
    nom: 'Benali',
    prenom: 'Youssef',
    email: 'youssef.benali@example.ma',
    telephone: null,
    adresse: null,
    dateNaissance: null,
  },
];

const FORMATIONS: Formation[] = [
  {
    id: 1,
    intitule: 'DevOps avec Docker et Kubernetes',
    duree: 40,
    capaciteMax: 25,
    statut: 'OUVERTE',
    placesRestantes: 23,
  },
  {
    id: 2,
    intitule: 'Spring Boot avancé',
    duree: 30,
    capaciteMax: 20,
    statut: 'FERMEE',
    placesRestantes: 19,
  },
];

const INSCRIPTIONS: Inscription[] = [
  {
    id: 1,
    dateInscription: '2026-11-10',
    statut: 'VALIDEE',
    etudiant: { id: 1, nom: 'El Amrani', prenom: 'Sara', email: 'sara.elamrani@example.ma' },
    formation: { id: 1, intitule: 'DevOps avec Docker et Kubernetes' },
  },
  {
    id: 2,
    dateInscription: '2026-11-12',
    statut: 'EN_ATTENTE',
    etudiant: { id: 2, nom: 'Benali', prenom: 'Youssef', email: 'youssef.benali@example.ma' },
    formation: { id: 1, intitule: 'DevOps avec Docker et Kubernetes' },
  },
];

const DONNEES: Record<string, { id: number }[]> = {
  etudiants: ETUDIANTS,
  formations: FORMATIONS,
  inscriptions: INSCRIPTIONS,
};

const LATENCE_MS = 300;

export const mockApiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!environment.useMock || !req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  // Exemple : /api/formations/1/etudiants -> ['formations', '1', 'etudiants']
  const [ressource, id, sousRessource] = req.url
    .substring(environment.apiUrl.length)
    .split('/')
    .filter((segment) => segment.length > 0);

  const liste = DONNEES[ressource];
  if (!liste) {
    return next(req);
  }

  if (req.method !== 'GET') {
    const status = req.method === 'POST' ? 201 : req.method === 'DELETE' ? 204 : 200;
    const body = req.method === 'DELETE' ? null : { id: Number(id ?? 99), ...(req.body as object) };
    return of(new HttpResponse({ status, body })).pipe(delay(LATENCE_MS));
  }

  if (!id) {
    const terme = req.params.get('recherche')?.toLowerCase();
    const resultat =
      ressource === 'etudiants' && terme
        ? ETUDIANTS.filter((e) => `${e.nom} ${e.prenom}`.toLowerCase().includes(terme))
        : liste;
    return of(new HttpResponse({ status: 200, body: resultat })).pipe(delay(LATENCE_MS));
  }

  const element = liste.find((e) => e.id === Number(id));
  if (!element) {
    const erreur = new HttpErrorResponse({
      status: 404,
      url: req.url,
      error: { title: 'Ressource introuvable', status: 404, code: 'RESSOURCE_INTROUVABLE' },
    });
    return throwError(() => erreur).pipe(delay(LATENCE_MS));
  }

  if (ressource === 'formations' && sousRessource === 'etudiants') {
    const inscrits = INSCRIPTIONS.filter(
      (i) => i.formation.id === element.id && i.statut !== 'ANNULEE',
    ).map((i) => ETUDIANTS.find((e) => e.id === i.etudiant.id));
    return of(new HttpResponse({ status: 200, body: inscrits })).pipe(delay(LATENCE_MS));
  }

  return of(new HttpResponse({ status: 200, body: element })).pipe(delay(LATENCE_MS));
};
