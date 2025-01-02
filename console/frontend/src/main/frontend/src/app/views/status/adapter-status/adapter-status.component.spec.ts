import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdapterStatusComponent } from './adapter-status.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AdapterStatusComponent', () => {
  let component: AdapterStatusComponent;
  let fixture: ComponentFixture<AdapterStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AdapterStatusComponent],
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AdapterStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
