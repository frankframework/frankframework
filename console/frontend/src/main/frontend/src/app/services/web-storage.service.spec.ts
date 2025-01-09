import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { WebStorageService } from './web-storage.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('WebStorageService', () => {
  let service: WebStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        {
          provide: Window,
          useValue: window,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(WebStorageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
