import { DropLastCharPipe } from './drop-last-char.pipe';

describe('DropLastCharPipe', () => {
  let pipe: DropLastCharPipe;

  beforeEach(() => {
    pipe = new DropLastCharPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('drops the last character', () => {
    expect(pipe.transform('This is a test')).toBe('This is a tes');
  });

  it('to do nothing on an empty string', () => {
    expect(pipe.transform('')).toBe('');
  });
});
