import { TestBed } from '@angular/core/testing';

import { ConfigurationsService } from './configurations.service';

describe('ConfigurationsService', () => {
  let service: ConfigurationsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConfigurationsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
