import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { SchedulerService } from './scheduler.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('SchedulerService', () => {
  let service: SchedulerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(SchedulerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
