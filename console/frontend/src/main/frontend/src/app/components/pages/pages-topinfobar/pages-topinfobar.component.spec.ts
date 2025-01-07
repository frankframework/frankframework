import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PagesTopinfobarComponent } from './pages-topinfobar.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PagesTopinfobarComponent', () => {
  let component: PagesTopinfobarComponent;
  let fixture: ComponentFixture<PagesTopinfobarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, PagesTopinfobarComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesTopinfobarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
