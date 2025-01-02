import { TestBed } from '@angular/core/testing';

import { JmsService } from './jms.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('JmsService', () => {
  let service: JmsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(JmsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
