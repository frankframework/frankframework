import { DomSanitizer } from '@angular/platform-browser';
import { MarkDownPipe } from './mark-down.pipe';
import { TestBed } from '@angular/core/testing';

describe('MarkDownPipe', () => {
  let domSanitizer: DomSanitizer;
  let pipe: MarkDownPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      /*
      providers: [
        {
          provide: DomSanitizer,
          useValue: {
            bypassSecurityTrustHtml: (html: string): string => html,
          },
        },
      ],
     */
    });
    domSanitizer = TestBed.inject(DomSanitizer);
    pipe = new MarkDownPipe(domSanitizer);
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });
});
