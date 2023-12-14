import { TestBed } from '@angular/core/testing';

import { JdbcService } from './jdbc.service';

describe('JdbcService', () => {
  let service: JdbcService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(JdbcService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
