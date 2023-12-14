import { TestBed } from '@angular/core/testing';

import { SweetalertService } from './sweetalert.service';

describe('SweetalertService', () => {
  let service: SweetalertService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SweetalertService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
