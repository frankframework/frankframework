import { TestBed } from '@angular/core/testing';

import { ServerTimeService } from './server-time.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ServerTimeService', () => {
  let service: ServerTimeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(ServerTimeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
