import { VariablesFilterPipe } from './variables-filter.pipe';

describe('VariablesFilterPipe', () => {
  let pipe: VariablesFilterPipe;

  beforeEach(() => {
    pipe = new VariablesFilterPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('to filter an array of strings', () => {
    const originalList = ['ab', 'bb', 'cc', 'cd', 'bc'];
    const expectedList = ['ab', 'bb', 'bc'];
    expect(pipe.transform(originalList, 'b')).toEqual(expectedList);
  });

  it('to filter numbers based on recurring number sequence', () => {
    const originalList = [1234, 2233, 1144, 1423, 1441];
    const expectedList = [1234, 2233, 1423];
    expect(pipe.transform(originalList, '2')).toEqual(expectedList);
  });

  it('to return an empty array if empty array is given', () => {
    expect(pipe.transform([], '')).toEqual([]);
  });

  it('to return an empty array when the filter input is not in the array', () => {
    const originalList = ['ab', 'bb', 'cc', 'cd', 'bc'];
    expect(pipe.transform(originalList, '1')).toEqual([]);
  });

  it('to return everything when the filter input is an empty string', () => {
    const originalList = ['ab', 'bb', 'cc', 'cd', 'bc'];
    expect(pipe.transform(originalList, '')).toEqual(originalList);
  });
});
