import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PollerService } from './poller.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PollerService', () => {
  let service: PollerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(PollerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
