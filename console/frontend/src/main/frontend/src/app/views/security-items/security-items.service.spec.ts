import { TestBed } from '@angular/core/testing';

import { SecurityItemsService } from './security-items.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('SecurityItemsService', () => {
  let service: SecurityItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(SecurityItemsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
