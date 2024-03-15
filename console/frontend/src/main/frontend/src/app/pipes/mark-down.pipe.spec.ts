import { DomSanitizer } from '@angular/platform-browser';
import { MarkDownPipe } from './mark-down.pipe';
import { TestBed } from '@angular/core/testing';

describe('MarkDownPipe', () => {
  let domSanitizer: DomSanitizer;
  let pipe: MarkDownPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    domSanitizer = TestBed.inject(DomSanitizer);
    pipe = new MarkDownPipe(domSanitizer);
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('return empty string if empty string is given', () => {
    expect(pipe.transform('')).toBe('');
  });

  it('gives back sanitized html', () => {
    const input = 'This is a test';
    expect(pipe.transform(input)).toBeTruthy();
    expect(typeof pipe.transform(input)).toBe('object'); // sanitizer returns SafeHTML thus an object
  });
});
