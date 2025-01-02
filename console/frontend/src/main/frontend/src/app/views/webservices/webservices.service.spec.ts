import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { WebservicesService } from './webservices.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('WebservicesService', () => {
  let service: WebservicesService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(WebservicesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
