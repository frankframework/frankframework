import { HumanFileSizePipe } from './human-file-size.pipe';

describe('HumanFileSizePipe', () => {
  let pipe: HumanFileSizePipe;

  beforeEach(() => {
    pipe = new HumanFileSizePipe();
  });

  it('create an instance', () => {
    const pipe = new HumanFileSizePipe();
    expect(pipe).toBeTruthy();
  });

  it('Transform 3 characters', () => {
    expect(pipe.transform(123)).toBe('123 B');
  });

  it('Transform 4 characters to binary (IEC)', () => {
    expect(pipe.transform(1234)).toBe('1.2 KiB');
  });

  it('Transform 8 characters to binary (IEC)', () => {
    expect(pipe.transform(12_345_678)).toBe('11.8 MiB');
  });

  it('Transform 8 characters to metric (SI)', () => {
    expect(pipe.transform(12_345_678, true)).toBe('12.3 MB');
  });

  it('Transform 8 characters to binary (IEC) with no decimals', () => {
    expect(pipe.transform(12_345_678, false, 0)).toBe('12 MiB');
  });

  it('Transform 8 characters to metric (SI) with no decimals', () => {
    expect(pipe.transform(12_345_678, true, 0)).toBe('12 MB');
  });

  it('Transform 8 characters to binary (IEC) with all decimals', () => {
    expect(pipe.transform(12_345_678, false, 6)).toBe('11.773756 MiB');
  });

  it('Transform 8 characters to metric (SI) with all decimals', () => {
    expect(pipe.transform(12_345_678, true, 6)).toBe('12.345678 MB');
  });
});
