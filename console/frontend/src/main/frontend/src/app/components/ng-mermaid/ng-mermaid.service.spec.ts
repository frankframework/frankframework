import { TestBed } from '@angular/core/testing';

import { NgMermaidService } from './ng-mermaid.service';

describe('NgMermaidService', () => {
  let service: NgMermaidService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NgMermaidService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
