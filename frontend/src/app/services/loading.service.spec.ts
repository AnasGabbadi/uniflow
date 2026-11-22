import { TestBed } from '@angular/core/testing';

import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  let service: LoadingService;

  beforeEach(() => {
    service = TestBed.inject(LoadingService);
  });

  it('est inactif au départ', () => {
    expect(service.actif()).toBe(false);
    expect(service.requetesEnCours()).toBe(0);
  });

  it('reste actif tant qu’une requête est en cours', () => {
    service.demarrer();
    service.demarrer();
    expect(service.requetesEnCours()).toBe(2);
    expect(service.actif()).toBe(true);

    service.terminer();
    expect(service.actif()).toBe(true);

    service.terminer();
    expect(service.actif()).toBe(false);
  });

  it('ne descend jamais sous zéro', () => {
    service.terminer();

    expect(service.requetesEnCours()).toBe(0);
    expect(service.actif()).toBe(false);
  });
});
