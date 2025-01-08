import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesNavigationComponent } from './pages-navigation.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { routes } from '../../../app.routes';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PagesNavigationComponent', () => {
  let component: PagesNavigationComponent;
  let fixture: ComponentFixture<PagesNavigationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      schemas: [NO_ERRORS_SCHEMA],
      imports: [PagesNavigationComponent],
      providers: [provideRouter(routes), provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesNavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
