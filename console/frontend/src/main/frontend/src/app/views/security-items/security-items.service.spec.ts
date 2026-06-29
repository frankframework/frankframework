import { TestBed } from '@angular/core/testing';

import { SecurityItemsService } from './security-items.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('SecurityItemsService', () => {
  let service: SecurityItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(SecurityItemsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
