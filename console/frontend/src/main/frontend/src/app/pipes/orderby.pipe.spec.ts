import { OrderByPipe } from './orderby.pipe';

describe('OrderByPipe', () => {
  let pipe: OrderByPipe;

  const inputList = [
    {
      name: 'test1',
      value: 'a',
    },
    {
      name: 'test2',
      value: 'z',
    },
    {
      name: 'test4',
      value: 'i',
    },
    {
      name: 'test3',
      value: 'j',
    },
  ];

  beforeEach(() => {
    pipe = new OrderByPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('returns an empty array if empty array is given', () => {
    expect(pipe.transform([], 'field')).toEqual([]);
  });

  it('sort a basic object list on "value" key', () => {
    const expectedList = [
      {
        name: 'test1',
        value: 'a',
      },
      {
        name: 'test4',
        value: 'i',
      },
      {
        name: 'test3',
        value: 'j',
      },
      {
        name: 'test2',
        value: 'z',
      },
    ];
    expect(pipe.transform(inputList, 'value')).toEqual(expectedList);
  });

  it('sort a basic object list on "name" key', () => {
    const expectedList = [
      {
        name: 'test1',
        value: 'a',
      },
      {
        name: 'test2',
        value: 'z',
      },
      {
        name: 'test3',
        value: 'j',
      },
      {
        name: 'test4',
        value: 'i',
      },
    ];
    expect(pipe.transform(inputList, 'name')).toEqual(expectedList);
  });
});
