import { TestBed } from '@angular/core/testing';

import { UiLoading } from './ui-loading';

describe('UiLoading', () => {
  it('affiche le libellé par défaut avec role="status" et aria-live', () => {
    const fixture = TestBed.createComponent(UiLoading);
    fixture.detectChanges();

    const statut = (fixture.nativeElement as HTMLElement).querySelector('[role="status"]');
    expect(statut?.getAttribute('aria-live')).toBe('polite');
    expect(statut?.textContent).toContain('Loading…');
  });

  it('affiche le libellé fourni', () => {
    const fixture = TestBed.createComponent(UiLoading);
    fixture.componentRef.setInput('label', 'Loading trainings…');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading trainings…');
  });
});
