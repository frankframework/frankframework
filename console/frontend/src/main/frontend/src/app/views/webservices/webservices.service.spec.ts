import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { WebservicesService } from './webservices.service';

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
