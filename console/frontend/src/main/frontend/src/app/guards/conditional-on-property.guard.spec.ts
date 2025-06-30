import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { conditionalOnPropertyGuard } from './conditional-on-property.guard';

describe('conditionalOnPropertyGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => conditionalOnPropertyGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
