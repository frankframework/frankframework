import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesNavigationComponent } from './pages-navigation.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { routes } from '../../../app-routing.module';

describe('PagesNavigationComponent', () => {
  let component: PagesNavigationComponent;
  let fixture: ComponentFixture<PagesNavigationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PagesNavigationComponent, HttpClientTestingModule],
      providers: [provideRouter(routes)],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesNavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
