import { TestBed } from '@angular/core/testing';
import { FormatStatKeysPipe } from './format-stat-keys.pipe';
import { KeyValueDiffers } from '@angular/core';

describe('FormatStatKeysPipe', () => {
  let keyValueDiffers: KeyValueDiffers;
  let pipe: FormatStatKeysPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    keyValueDiffers = TestBed.inject(KeyValueDiffers);
    pipe = new FormatStatKeysPipe(keyValueDiffers);
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });
});
