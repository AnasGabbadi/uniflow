import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';

import { UiFormField } from './ui-form-field';

@Component({
  imports: [UiFormField],
  template: `<app-ui-form-field label="Email" controlId="email" [required]="true">
    <input id="email" type="email" />
  </app-ui-form-field>`,
})
class HoteFormField {}

describe('UiFormField', () => {
  let component: UiFormField;
  let fixture: ComponentFixture<UiFormField>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiFormField],
    }).compileComponents();

    fixture = TestBed.createComponent(UiFormField);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('relie le label au champ et projette le contenu', () => {
    const hote = TestBed.createComponent(HoteFormField);
    hote.detectChanges();
    const element = hote.nativeElement as HTMLElement;

    const label = element.querySelector('label');
    expect(label?.getAttribute('for')).toBe('email');
    expect(label?.textContent).toContain('Email');
    expect(element.querySelector('input#email')).not.toBeNull();
  });

  it("affiche l'astérisque seulement pour un champ obligatoire", () => {
    fixture.componentRef.setInput('label', 'Phone');
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.required')).toBeNull();

    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();
    const etoile = element.querySelector('.required');
    expect(etoile?.textContent).toContain('*');
    expect(etoile?.getAttribute('aria-hidden')).toBe('true');
  });
});
