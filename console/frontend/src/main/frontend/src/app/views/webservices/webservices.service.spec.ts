import { TestBed } from '@angular/core/testing';

import { WebservicesService } from './webservices.service';

describe('WebservicesService', () => {
  let service: WebservicesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WebservicesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
