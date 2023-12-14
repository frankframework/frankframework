import { TestBed } from '@angular/core/testing';

import { MonitorsService } from './monitors.service';

describe('MonitorsService', () => {
  let service: MonitorsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MonitorsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
