import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { JdbcService } from './jdbc.service';

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
