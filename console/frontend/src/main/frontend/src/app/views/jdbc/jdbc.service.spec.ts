import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { JdbcService } from './jdbc.service';

describe('JdbcService', () => {
  let service: JdbcService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(JdbcService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
