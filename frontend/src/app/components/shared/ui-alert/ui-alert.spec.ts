import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UiAlert } from './ui-alert';

describe('UiAlert', () => {
  let fixture: ComponentFixture<UiAlert>;
  let element: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(UiAlert);
    element = fixture.nativeElement as HTMLElement;
    fixture.componentRef.setInput('message', 'Unable to reach the server.');
  });

  it('affiche une erreur avec role="alert" et sans bouton par défaut', () => {
    fixture.detectChanges();

    const alerte = element.querySelector('[role="alert"]');
    expect(alerte?.textContent).toContain('Unable to reach the server.');
    expect(alerte?.getAttribute('data-variant')).toBe('error');
    expect(element.querySelector('button')).toBeNull();
  });

  it('affiche un succès avec role="status"', () => {
    fixture.componentRef.setInput('variant', 'success');
    fixture.detectChanges();

    expect(element.querySelector('[role="status"]')?.getAttribute('data-variant')).toBe('success');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('émet retry quand on clique sur « Retry »', () => {
    fixture.componentRef.setInput('retryable', true);
    fixture.detectChanges();
    const espion = vi.fn();
    fixture.componentInstance.retry.subscribe(espion);

    const bouton = element.querySelector('button') as HTMLButtonElement;
    expect(bouton.textContent).toContain('Retry');
    bouton.click();

    expect(espion).toHaveBeenCalledTimes(1);
  });
});
