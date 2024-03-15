import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { AdapterstatisticsService } from './adapterstatistics.service';

describe('AdapterstatisticsService', () => {
  let service: AdapterstatisticsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(AdapterstatisticsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
