import { TestBed } from '@angular/core/testing';

import { ConfigurationsService } from './configurations.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ConfigurationsService', () => {
  let service: ConfigurationsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ConfigurationsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
