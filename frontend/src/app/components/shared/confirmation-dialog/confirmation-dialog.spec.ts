import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmationDialog } from './confirmation-dialog';

describe('ConfirmationDialog', () => {
  let component: ConfirmationDialog;
  let fixture: ComponentFixture<ConfirmationDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmationDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmationDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it("n'affiche rien tant que open vaut false", () => {
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alertdialog"]')).toBeNull();
  });

  it('affiche le titre et le message quand open vaut true', () => {
    fixture.componentRef.setInput('open', true);
    fixture.componentRef.setInput('title', 'Delete training');
    fixture.componentRef.setInput('message', 'Delete the training "DevOps"?');
    fixture.detectChanges();

    const dialogue = (fixture.nativeElement as HTMLElement).querySelector('[role="alertdialog"]');
    expect(dialogue?.getAttribute('aria-modal')).toBe('true');
    expect(dialogue?.querySelector('h2')?.textContent).toContain('Delete training');
    expect(dialogue?.textContent).toContain('Delete the training "DevOps"?');
  });

  it('émet confirmed sur « Confirm » et cancelled sur « Cancel »', () => {
    fixture.componentRef.setInput('open', true);
    fixture.detectChanges();
    const confirme = vi.fn();
    const annule = vi.fn();
    component.confirmed.subscribe(confirme);
    component.cancelled.subscribe(annule);
    const element = fixture.nativeElement as HTMLElement;

    (element.querySelector('button.confirm') as HTMLButtonElement).click();
    expect(confirme).toHaveBeenCalledTimes(1);
    expect(annule).not.toHaveBeenCalled();

    (element.querySelector('button.cancel') as HTMLButtonElement).click();
    expect(annule).toHaveBeenCalledTimes(1);
  });
});
