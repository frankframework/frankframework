import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PollerService } from './poller.service';

describe('PollerService', () => {
  let service: PollerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(PollerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
