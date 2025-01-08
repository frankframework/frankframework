import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { MonitorsService } from './monitors.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('MonitorsService', () => {
  let service: MonitorsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(MonitorsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
