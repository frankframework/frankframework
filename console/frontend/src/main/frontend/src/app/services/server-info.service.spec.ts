import { TestBed } from '@angular/core/testing';

import { ServerInfoService } from './server-info.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ServerInfoService', () => {
  let service: ServerInfoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    });
    service = TestBed.inject(ServerInfoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
