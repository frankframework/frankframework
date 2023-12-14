import { TestBed } from '@angular/core/testing';

import { SecurityItemsService } from './security-items.service';

describe('SecurityItemsService', () => {
  let service: SecurityItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SecurityItemsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
