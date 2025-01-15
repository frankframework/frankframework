import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AdapterstatisticsService } from './adapterstatistics.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AdapterstatisticsService', () => {
  let service: AdapterstatisticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdapterstatisticsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
