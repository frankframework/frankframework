import { TestBed } from '@angular/core/testing';

import { ServerTimeService } from './server-time.service';

describe('ServerTimeService', () => {
  let service: ServerTimeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ServerTimeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
