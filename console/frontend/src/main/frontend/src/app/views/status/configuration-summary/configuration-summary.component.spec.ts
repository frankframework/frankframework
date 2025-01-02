import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationSummaryComponent } from './configuration-summary.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ConfigurationSummaryComponent', () => {
  let component: ConfigurationSummaryComponent;
  let fixture: ComponentFixture<ConfigurationSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfigurationSummaryComponent],
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigurationSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
