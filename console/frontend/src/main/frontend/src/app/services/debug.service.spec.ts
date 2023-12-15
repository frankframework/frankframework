import { TestBed } from '@angular/core/testing';

import { DebugService } from './debug.service';

describe('DebugService', () => {
  let service: DebugService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DebugService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
