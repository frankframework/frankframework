import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { MonitorsService } from './monitors.service';

describe('MonitorsService', () => {
  let service: MonitorsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(MonitorsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
