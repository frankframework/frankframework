import { TestBed } from '@angular/core/testing';

import { ServerInfoService } from './server-info.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ServerInfoService', () => {
  let service: ServerInfoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ServerInfoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
