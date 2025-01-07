import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { JdbcService } from './jdbc.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('JdbcService', () => {
  let service: JdbcService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(JdbcService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
