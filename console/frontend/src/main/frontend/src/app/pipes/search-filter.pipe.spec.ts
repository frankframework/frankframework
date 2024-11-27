import { SearchFilterPipe } from './search-filter.pipe';

describe('SearchFilterPipe', () => {
  let pipe: SearchFilterPipe;

  beforeEach(() => {
    pipe = new SearchFilterPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('returns an empty array if empty string is given', () => {
    expect(pipe.transform([], '')).toEqual([]);
  });

  it('returns an empty array if the original array is empty', () => {
    expect(pipe.transform([], 'test')).toEqual([]);
  });

  it('returns an object with filtered values', () => {
    const originalRecords = {
      a: '1djwja',
      b: 'duduahd1iid',
      c: '',
      d: 'kjwajdkwajd',
      one1: 'one',
    };
    const expectedList = {
      a: '1djwja',
      b: 'duduahd1iid',
    };
    expect(pipe.transform(originalRecords, '1')).toEqual(expectedList);
  });

  it('returns an array of filtered values', () => {
    const originalArray = ['1djwja', 'duduahd1iid', '', 'kjwajdkwajd', 'one'];
    const expectedList = ['1djwja', 'duduahd1iid'];
    expect(pipe.transform(originalArray, '1')).toEqual(expectedList);
  });
});
