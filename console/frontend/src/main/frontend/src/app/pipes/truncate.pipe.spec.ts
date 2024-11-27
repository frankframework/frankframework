import { TruncatePipe } from './truncate.pipe';

describe('TruncatePipe', () => {
  let pipe: TruncatePipe;

  beforeEach(() => {
    pipe = new TruncatePipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it("doesn't truncate text shorter than minimum length", () => {
    expect(pipe.transform('This is a test', 15)).toBe('This is a test');
  });

  it("doesn't truyncate text equal to minimum length", () => {
    expect(pipe.transform('This is the minimum length', 26)).toBe('This is the minimum length');
  });

  it('truncates text longer than minimum length', () => {
    expect(pipe.transform('This is a test', 10)).toBe('This is a ... (4 characters more)');
  });
});
