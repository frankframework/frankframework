import { TestBed } from '@angular/core/testing';

import { JmsService } from './jms.service';

describe('JmsService', () => {
  let service: JmsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(JmsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
