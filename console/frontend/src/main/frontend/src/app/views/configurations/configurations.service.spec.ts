import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { ConfigurationsService } from './configurations.service';

describe('ConfigurationsService', () => {
  let service: ConfigurationsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(ConfigurationsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
