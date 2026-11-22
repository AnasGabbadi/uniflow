import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiButton } from './ui-button';

describe('UiButton', () => {
  let component: UiButton;
  let fixture: ComponentFixture<UiButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiButton],
    }).compileComponents();

    fixture = TestBed.createComponent(UiButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche le libellé et la variante', () => {
    fixture.componentRef.setInput('label', 'New training');
    fixture.componentRef.setInput('variant', 'danger');
    fixture.detectChanges();

    const bouton = (fixture.nativeElement as HTMLElement).querySelector('button');
    expect(bouton?.textContent).toContain('New training');
    expect(bouton?.getAttribute('data-variant')).toBe('danger');
    expect(bouton?.type).toBe('button');
  });

  it('émet clicked au clic', () => {
    fixture.detectChanges();
    const espion = vi.fn();
    component.clicked.subscribe(espion);

    ((fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement).click();

    expect(espion).toHaveBeenCalledTimes(1);
  });

  it("n'émet pas clicked quand il est désactivé", () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    const espion = vi.fn();
    component.clicked.subscribe(espion);
    const bouton = (fixture.nativeElement as HTMLElement).querySelector(
      'button',
    ) as HTMLButtonElement;

    bouton.click();

    expect(bouton.disabled).toBe(true);
    expect(espion).not.toHaveBeenCalled();
  });
});
