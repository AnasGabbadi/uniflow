import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiFormField } from './ui-form-field';

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
});
