import { TestBed } from '@angular/core/testing';

import { JmsService } from './jms.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('JmsService', () => {
  let service: JmsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(JmsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
