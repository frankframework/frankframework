import { TestBed } from '@angular/core/testing';

import { ServerTimeService } from './server-time.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ServerTimeService', () => {
  let service: ServerTimeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ServerTimeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
