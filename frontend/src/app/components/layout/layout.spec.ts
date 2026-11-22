import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LoadingService } from '../../services/loading.service';
import { Layout } from './layout';

describe('Layout', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(Layout);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche la barre de progression globale pendant les requêtes HTTP', () => {
    const fixture = TestBed.createComponent(Layout);
    const chargement = TestBed.inject(LoadingService);
    const element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    expect(element.querySelector('.progress-bar')).toBeNull();

    chargement.demarrer();
    fixture.detectChanges();
    expect(element.querySelector('.progress-bar')).not.toBeNull();

    chargement.terminer();
    fixture.detectChanges();
    expect(element.querySelector('.progress-bar')).toBeNull();
  });
});
