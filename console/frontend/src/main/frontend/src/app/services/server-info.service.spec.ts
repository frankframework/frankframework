import { TestBed } from '@angular/core/testing';

import { ServerInfoService } from './server-info.service';

describe('ServerInfoService', () => {
  let service: ServerInfoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ServerInfoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
