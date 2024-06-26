import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { WebservicesService } from './webservices.service';

describe('WebservicesService', () => {
  let service: WebservicesService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(WebservicesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
