import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { CustomViewsComponent } from './custom-views.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CustomViewsComponent', () => {
  let component: CustomViewsComponent;
  let fixture: ComponentFixture<CustomViewsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomViewsComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomViewsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
