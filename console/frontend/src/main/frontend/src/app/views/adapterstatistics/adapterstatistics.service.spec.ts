import { TestBed } from '@angular/core/testing';

import { AdapterstatisticsService } from './adapterstatistics.service';

describe('AdapterstatisticsService', () => {
  let service: AdapterstatisticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AdapterstatisticsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
